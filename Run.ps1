param([switch]$Test, [switch]$BuildOnly)
$ErrorActionPreference = 'Stop'
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw 'Install a JDK (Java 21) and make javac available on PATH.' }
$root = $PSScriptRoot
$buildFolder = Join-Path $root 'out'
New-Item -ItemType Directory -Path $buildFolder -Force | Out-Null
$sourcePaths = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
if ($Test) { $sourcePaths += @(Get-ChildItem -LiteralPath (Join-Path $root 'test') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }) }
$argumentFile = Join-Path $buildFolder 'sources.txt'
$quotedSources = $sourcePaths | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }
[IO.File]::WriteAllLines($argumentFile, $quotedSources, (New-Object System.Text.UTF8Encoding($false)))
& javac -encoding UTF-8 -d $buildFolder "@$argumentFile"
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Read the Java error above.' }
if ($BuildOnly) { Write-Output 'Build passed.'; exit 0 }
if ($Test) {
    & java '-Djava.awt.headless=true' -classpath $buildFolder ui.WorkflowChecks
} else {
    $dataFile = Join-Path $root 'data\kouzina.dat'
    & java "-Dkouzina.data=$dataFile" -classpath $buildFolder KouzinaApp
}
if ($LASTEXITCODE -ne 0) { throw 'The program exited with an error.' }
