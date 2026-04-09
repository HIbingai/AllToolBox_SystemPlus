$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$wrapperDir = Join-Path $root 'gradle/wrapper'
New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null

$gradleTag = 'v8.11.1'
$rawBase = "https://raw.githubusercontent.com/gradle/gradle/$gradleTag"

$gradlewPath = Join-Path $root 'gradlew'
$gradlewBatPath = Join-Path $root 'gradlew.bat'
$wrapperJarPath = Join-Path $wrapperDir 'gradle-wrapper.jar'
$wrapperPropsPath = Join-Path $wrapperDir 'gradle-wrapper.properties'
$localZipPath = Join-Path $wrapperDir 'gradle-8.11.1-bin.zip'

Invoke-WebRequest -Uri "$rawBase/gradlew" -OutFile $gradlewPath -UseBasicParsing
Invoke-WebRequest -Uri "$rawBase/gradlew.bat" -OutFile $gradlewBatPath -UseBasicParsing
Invoke-WebRequest -Uri "$rawBase/gradle/wrapper/gradle-wrapper.jar" -OutFile $wrapperJarPath -UseBasicParsing

if (-not (Test-Path $localZipPath)) {
	# Curl with disabled revocation check is more reliable on some locked-down Windows hosts.
	& curl.exe --ssl-no-revoke -L 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' -o $localZipPath | Out-Null
}

$distributionUrl = if (Test-Path $localZipPath) {
	'file:///' + ($localZipPath -replace '\\', '/')
} else {
	'https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip'
}

@'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=__DISTRIBUTION_URL__
networkTimeout=10000
validateDistributionUrl=false
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
'@.Replace('__DISTRIBUTION_URL__', $distributionUrl) | Set-Content -Path $wrapperPropsPath -Encoding ASCII

Write-Output 'BOOTSTRAP_OK: gradlew, gradlew.bat and wrapper jar/properties are ready.'
