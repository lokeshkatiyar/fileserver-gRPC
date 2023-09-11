package bits.pilani;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;


@GrpcService
public class FileGrpcService implements FileGrpc {
	@Inject
	private UtilConstants utilConstants;

	@Override
	public Uni<FileContent> findFile(FileRequest request) {
		try {
			File file = utilConstants.searchFileInLocalSystem(request.getPath());
			String str = Files.readString(file.toPath());
			return Uni.createFrom().item(str)
					.map(s -> FileContent.newBuilder().setContent(s).build());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

//	@Override
//	public Uni<FileCompareResponse> compareFile(FileCompareRequest request) {
////		Path filePath = Files.createTempFile("receiveFile",".txt");
////		Files.writeString(filePath,)
////		utilConstants.isFilesIdenticalIgnoreEOF(request.getFirstContent().)
//		return null;
//	}

	@Override
	public Uni<PingResponse> ping(PingRequest request) {
		return Uni.createFrom().item("Hello " + request.getName() + "!")
				.map(msg -> PingResponse.newBuilder().setMessage(msg).build());
	}

}
