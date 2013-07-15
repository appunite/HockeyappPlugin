package org.gradle

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.artifacts.ArtifactPublicationServices
import org.gradle.api.publish.internal.PublishOperation
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.Input;
import java.util.concurrent.Callable;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import groovyx.net.http.*

import javax.inject.Inject
import java.util.zip.GZIPInputStream

import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.client.methods.HttpPost;

public class UploadHockeyapp extends DefaultTask {
    {
        description = 'Updates your application hockeyapp'
    }
    @Input
    String appKey = null
    @Input
    String appStatus = null
    @Input
    String appNotify = null
    @Input
    String appNotes = null
    @Input
    String appNotesType = null
    @Input
    String appReleaseType = null
    @Input
    String appTags = null

    @InputFile
    File appIpaFile = null

    @InputFile
    @Optional
    File appDsymFile = null
    @Input
    String appToken = null

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        doPublish()
    }

    private void doPublish() {
        def appIpaInputStream = new FileInputStream(appIpaFile);
        try {
            if (appDsymFile == null) {
                download(appIpaInputStream, null)
            } else {
                def appDsymInputStream = new FileInputStream(appDsymFile)
                try {
                    download(appIpaInputStream, appDsymInputStream)
                } catch (IOException e) {
                    appIpaInputStream.close();
                }
            }
        } catch (IOException e) {
            appDsymInputStream.close();
        }
    }

    private void download(FileInputStream appIpaInputStream, FileInputStream appDsymInputStream) {

        def url = "https://rink.hockeyapp.net/api/2/apps/${appKey}/app_versions"

        def appIpa = new InputStreamBody(appIpaInputStream, "application/octet-stream", "app.apk") {
            public long getContentLength() {
                return super.appIpaFile.size();
            }
        }


        HTTPBuilder http = new HTTPBuilder(url)

        http.handler.failure = { resp ->
            getLogger().info("My response handler got response: ${resp.statusLine}")
            getLogger().info("Response length: ${resp.headers.'Content-Length'}")
            throw new RuntimeException("Unexpected failure: ${resp.statusLine}")
        }

        MultipartEntity multipart = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
        multipart.addPart('status', new StringBody(appStatus))
        multipart.addPart('notify', new StringBody(appNotify))
        multipart.addPart('notes', new StringBody(appNotes))
        multipart.addPart('notes_type', new StringBody(appNotesType))
        multipart.addPart('release_type', new StringBody(appReleaseType))
        multipart.addPart('tags', new StringBody(appTags))
        multipart.addPart('ipa', appIpa)
        if (appDsymInputStream != null) {
            def appDsym = new InputStreamBody(appDsymInputStream, "application/octet-stream", "app.txt") {
                public long getContentLength() {
                    return super.appDsymFile.size();
                }
            }
            multipart.addPart('dsym', appDsym)
        }

        HttpPost request = new HttpPost(url)
        request.setHeader("Accept", "application/json")
        request.setHeader("X-HockeyAppToken", appToken)
        request.setEntity(multipart)
        @SuppressWarnings("deprecated")
        def client = new DefaultHttpClient();
        def response = client.execute(request)
        def statusCode = response.getStatusLine().getStatusCode()
        if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED) {
            getLogger().info("Response: " + getStringFromResponse(response))
            throw new RuntimeException("Wrong response: " + statusCode);
        }
    }

    public static String getStringFromResponse(HttpResponse response)
            throws IOException {

        def entity = response.getEntity();

        def contentEncoding = response.getFirstHeader("Content-Encoding");
        if (contentEncoding != null
                && contentEncoding.getValue().equalsIgnoreCase("gzip")) {

            InputStream content = entity.getContent();
            try {
                InputStream inStream = new GZIPInputStream(content);
                try {
                    return getStringFromInputStream(inStream);
                } finally {
                    inStream.close();
                }
            } finally {
                content.close();
            }
        } else {
            return EntityUtils.toString(entity);
        }
    }

}
