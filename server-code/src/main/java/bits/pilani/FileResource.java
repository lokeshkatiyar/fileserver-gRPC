package bits.pilani;

import io.quarkus.grpc.GrpcClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Path("/file")
public class FileResource {
	@GrpcClient
	private FileGrpc fileService;
	@Inject
	private UtilConstants utilConstants;

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String searchFIle(@QueryParam("filename") String fileName) throws FileNotFoundException {
		File file = utilConstants.searchFileInLocalSystem(fileName);
		System.out.println(file.getAbsoluteFile());
		return file.getAbsolutePath();
	}

	@POST
	@Path("/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Consumes("application/x-www-form-urlencoded")
	public Response downloadFileWithPost(@FormParam("file") String fileName) {
		List<File> files = new ArrayList<>();
		try {
			File localFile = utilConstants.searchFileInLocalSystem(fileName);
			//get the file content from the other servers
			File otherServerFile = utilConstants.hitOtherServer(fileName);
			//compare both the files
			if (Objects.nonNull(localFile) && Objects.nonNull(otherServerFile)) {
				boolean filesIdentical = utilConstants.isFilesIdenticalIgnoreEOF(localFile, otherServerFile);
				if (filesIdentical) {
					files.add(localFile);
					files.add(otherServerFile);
				} else {
					files.add(localFile);
				}
			} else if (Objects.nonNull(localFile)) {
				files.add(localFile);
			} else if (Objects.nonNull(otherServerFile)) {
				files.add(otherServerFile);
			}

			//zip both the files, based on the conditions given in the assignment
			if (!files.isEmpty()) {
				File file = utilConstants.zipFiles(files);
				Response.ResponseBuilder response ;
				if(files.size()==1){
					response = Response.ok((Object) file);
					response.header("Content-Disposition", "attachment;filename=" + file.getName());
					response.header("msg", "File is available on one server");
				}else{
					response = Response.ok((Object) file);
					response.header("Content-Disposition", "attachment;filename=" + file.getName());
					response.header("msg", "File is available on all the servers");
				}
				return response.build();
			} else {
				Response.ResponseBuilder response = Response.noContent().status(Response.Status.NO_CONTENT.getStatusCode(), "File not found on servers").entity("File not found on servers");
				response.header("msg", "File not found on servers");
				return response.build();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
