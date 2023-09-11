package bits.pilani;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class UtilConstants {
	@GrpcClient
	FileGrpc fileService;

	@ConfigProperty(name = "fileserver.path")
	String fileserver;

	public boolean isFilesIdenticalIgnoreEOF(File file1, File file2) {
		try (Reader reader1 = new BufferedReader(new FileReader(file1));
		     Reader reader2 = new BufferedReader(new FileReader(file2))) {
			return IOUtils.contentEqualsIgnoreEOL(reader1, reader2);
		} catch (IOException e) {
			return false;
		}
	}

	public File searchFileInLocalSystem(String filename) {
		File file = Paths.get(fileserver, filename).toFile();
		if (file.exists()) {
			return file;
		} else {
			return null;
		}
	}

	public File hitOtherServer(String filename) {
		Uni<String> transform =
				fileService.findFile(FileRequest.newBuilder().setPath(filename).build()).onItem().transform(pingResponse -> pingResponse.getContent());
		String tempFileName = "otherServerFile.txt";
		try {
			Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir"), tempFileName));
			File file = new File(System.getProperty("java.io.tmpdir"), tempFileName);
			Files.writeString(file.toPath(), transform.await().atMost(Duration.ofSeconds(100)));
			return file;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			return null;
		} finally {

		}
	}

	public File zipFiles(List<File> srcFiles) throws IOException {
		Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir"), "compressed.zip"));
		File zipFile = new File(System.getProperty("java.io.tmpdir"), "compressed.zip");
		try (FileOutputStream fos = new FileOutputStream(zipFile.getAbsolutePath()); ZipOutputStream zipOut =
				new ZipOutputStream(fos);) {
			for (File fileToZip : srcFiles) {
				FileInputStream fis = new FileInputStream(fileToZip);
				ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
				zipOut.putNextEntry(zipEntry);

				byte[] bytes = new byte[1024];
				int length;
				while ((length = fis.read(bytes)) >= 0) {
					zipOut.write(bytes, 0, length);
				}
				fis.close();
			}
			return Paths.get(System.getProperty("java.io.tmpdir"), "compressed.zip").toFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
