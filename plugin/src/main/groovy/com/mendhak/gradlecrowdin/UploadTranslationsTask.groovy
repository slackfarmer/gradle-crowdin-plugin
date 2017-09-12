package com.mendhak.gradlecrowdin

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class UploadTranslationsTask extends DefaultTask {
  def apiKey
  def projectId
  def files
  def language
  def autoApproveImported

  static String crowdinURL = 'https://api.crowdin.com'

  @SuppressWarnings(['unchecked', 'GrUnresolvedAccess'])
  @TaskAction
  def uploadLanguageTranslations() {
    def updateFilePath = "${crowdinURL}/api/project/${projectId}/upload-translation?key=${apiKey}"

    def http = new HTTPBuilder(updateFilePath)

    http.handler.failure = { resp, reader ->
      println "Could not upload translations: ${resp.statusLine}"
      println reader
      throw new GradleException("Could not upload file: ${resp.statusLine} \r\n " + reader)
    }

    http.request(Method.POST, ContentType.ANY) { req ->
      MultipartEntityBuilder entity = MultipartEntityBuilder.create()
      files.each { pair ->
        def file = new File(pair[1])
        entity.addPart("files[${pair[0]}]", new FileBody(file))
      }
      entity.addPart('language', new StringBody(language))
      entity.addPart('auto_approve_imported', new StringBody(autoApproveImported))
      req.entity = entity.build()

      response.success = { resp, reader ->
        println "Uploaded ${files.size()} translation files to crowdin"
        println "Got response: ${resp.statusLine}"
        println "Content-Type: ${resp.headers.'Content-Type'}"
        println "Response text:[${reader.text}]"
      }
    }
  }
}
