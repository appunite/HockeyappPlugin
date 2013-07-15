package org.gradle

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class UploadHockeyappTest {
    @Test
    public void canAddTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('upload', type: UploadHockeyapp)
        assertTrue(task instanceof UploadHockeyapp)
        UploadHockeyapp uploadTask = (UploadHockeyapp)task;
        File apkFile = new File("some.apk")
        uploadTask.appDsymFile project.file(apkFile)
        uploadTask.appIpaFile project.file(apkFile)
        uploadTask.appKey = 'some_api_key'
        uploadTask.appStatus = "1"
        uploadTask.appNotify = "1"
        uploadTask.appNotes = "long long data"
        uploadTask.appNotesType = "1"
        uploadTask.appReleaseType = "2"
        uploadTask.appTags = "prebuild"
        uploadTask.appToken = "some_app_token"

        uploadTask.execute()
    }
}
