package com.mendhak.gradlecrowdin

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class UploadSourceFileTask extends DefaultTask {
  def apiKey
  def projectId
  def files
  def branch

  static String crowdinURL = 'https://api.crowdin.com'

  @SuppressWarnings(['unchecked', 'GrUnresolvedAccess'])
  @TaskAction
  def uploadSourceFile() {
    def branchEncoded = URLEncoder.encode((branch ?: '').replaceAll('[\\\\/:*?"<>|]', '_'), 'UTF-8')
    def uploadType
    if (branch != null && createBranch(branchEncoded)) {
      uploadType = 'add'
    } else {
      uploadType = 'update'
    }

    def updateFilePath = "${crowdinURL}/api/project/${projectId}/${uploadType}-file?key=${apiKey}"

    if (branch != null) {
      updateFilePath += "&branch=${branchEncoded}"
    }

    new HTTPBuilder(updateFilePath).request(Method.POST, ContentType.ANY) { req ->
      MultipartEntityBuilder entity = MultipartEntityBuilder.create()
      files.each { file ->
        entity.addPart("files[${file.name}]", new FileBody(new File(file.source)))
        if (uploadType == 'add' && file.title != null) {
          entity.addTextBody("titles[${file.name}]", file.title)
        }
        if (uploadType == 'add' && file.translation != null) {
          entity.addTextBody("export_patterns[${file.name}]", '/' + file.translation)
        }
      }
      req.entity = entity.build()

      response.failure = { resp, reader ->
        println "Could not upload file: ${resp.statusLine}"
        println reader
        throw new GradleException("Could not upload file: ${resp.statusLine} \r\n " + reader)
      }
      response.success = { resp, json ->
        println "Uploaded ${files.size()} files to crowdin"
      }
    }
  }

  /**
   * Creates the given branch on crowdin.
   * @return true if the new branch has been successfully created and false if it already existed.
   * */
  private boolean createBranch(String branchEncoded) {
    def created = false
    def addBranchPath = "${crowdinURL}/api/project/${projectId}/add-directory?key=${apiKey}&name" +
        "=${branchEncoded}&is_branch=1"

    new HTTPBuilder(addBranchPath).request(Method.POST, ContentType.ANY) { req ->
      response.failure = { resp, reader ->
        if (reader.code.text() != '50') {
          // Code 50 indicates that the branch already exists
          println "Could not create branch: ${resp.statusLine}"
          throw new GradleException("Could not create branch: ${resp.statusLine} \r\n " + reader)
        }
      }
      response.success = { resp, json ->
        println "Created branch $branch"
        created = true
      }
    }
    return created
  }
}
