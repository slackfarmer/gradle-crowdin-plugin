package com.mendhak.gradlecrowdin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


class DownloadTranslationsTask extends DefaultTask {
  def destination
  def apiKey
  def projectId
  def renameMapping
  def excludePattern
  def branch

  static String crowdinURL = 'https://api.crowdin.com'

  @TaskAction
  def downloadTranslations() {
    def branchEncoded = URLEncoder.encode((branch ?: '').replaceAll('[\\\\/:*?"<>|]', '_'), "UTF-8")

    def buildSubDir = new File(project.buildDir.getPath(), '/crowdin-plugin')
    buildSubDir.mkdirs()

    //Tell Crowdin to do an export
    def exportUrl = "${crowdinURL}/api/project/${projectId}/export?key=${apiKey}"
    if (branch != null) {
      exportUrl += '&branch=' + branchEncoded
    }

    ant.get(src: exportUrl, dest: new File(buildSubDir.getPath(), 'export.xml'), verbose: true)

    //Download actual zip file
    def url = "${crowdinURL}/api/project/${projectId}/download/all.zip?key=${apiKey}"

    if (branch != null) {
      url += "&branch=${branchEncoded}"
    }
    def translationZip = new File(buildSubDir.getPath(), 'translations.zip')
    ant.get(src: url, dest: translationZip, verbose: 'on')
    println 'Translations downloaded'

    //Extract
    def extractedDir = new File(buildSubDir, 'res')
    ant.unzip(src: translationZip, dest: extractedDir, overwrite: true)

    //Copy to destination
    if (excludePattern == null) {
      excludePattern = ''
    }

    ant.copy(todir: destination, overwrite: true, verbose: true) {
      fileset(dir: extractedDir, includes: '**/**', excludes: excludePattern)
      if (renameMapping != null) {
        regexpmapper(renameMapping)
      }
      println 'Copying to destination directory'
    }

    println 'Done'
  }
}
