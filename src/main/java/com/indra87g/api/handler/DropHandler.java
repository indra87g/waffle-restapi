package com.indra87g.api.handler;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import cn.nukkit.utils.Config;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Path("/drop")
public class DropHandler {

    @Context
    private Config config;
    private final Gson gson = new Gson();

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handle(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {

        String dropFolder = config.getString("drop-folder", "uploads");
        int maxFileSize = config.getInt("max-file-size", 10);
        long maxFileSizeBytes = maxFileSize * 1024L * 1024L;
        String fileName = new File(fileDetail.getFileName()).getName();
        String output_file = dropFolder + File.separator + fileName;

        try {
            File directory = new File(dropFolder);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(output_file);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                while ((bytesRead = uploadedInputStream.read(buffer)) != -1) {
                    totalBytesRead += bytesRead;
                    if (totalBytesRead > maxFileSizeBytes) {
                        fos.close();
                        file.delete();
                        return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                                .entity(gson.toJson(new ErrorResponse("File size exceeds the limit of " + maxFileSize + "MB")))
                                .build();
                    }
                    fos.write(buffer, 0, bytesRead);
                }
            }
            return Response.ok(gson.toJson(new SuccessResponse("File uploaded successfully to " + output_file))).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson(new ErrorResponse("Error uploading file: " + e.getMessage()))).build();
        }
    }

    @Getter
    @AllArgsConstructor
    private static class ErrorResponse {
        private final String error;
    }

    @Getter
    @AllArgsConstructor
    private static class SuccessResponse {
        private final String message;
    }
}