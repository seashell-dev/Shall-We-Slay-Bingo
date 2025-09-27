package net.runelite.client.plugins.bingo;

import okhttp3.*;
import java.io.File;
import java.io.IOException;

public class DiscordUploader
{
    private static final OkHttpClient client = new OkHttpClient();

    public static void sendFile(String webhookUrl, File file, String message) throws IOException
    {
        if (webhookUrl == null || webhookUrl.isEmpty())
        {
            throw new IllegalArgumentException("Webhook URL is empty");
        }

        // ✅ Correct ordering for OkHttp 4.x
        RequestBody fileBody = RequestBody.create(MediaType.parse("image/png"), file);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody);

        if (message != null && !message.isEmpty())
        {
            builder.addFormDataPart("content", message);
        }

        RequestBody requestBody = builder.build();
        Request request = new Request.Builder().url(webhookUrl).post(requestBody).build();

        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new IOException("Failed to send file: " + response);
            }
        }
    }
}
