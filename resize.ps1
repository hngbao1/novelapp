Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile('e:\bot\novel\icon.jpeg')

$sizes = @{
    'mipmap-mdpi' = 48
    'mipmap-hdpi' = 72
    'mipmap-xhdpi' = 96
    'mipmap-xxhdpi' = 144
    'mipmap-xxxhdpi' = 192
}

foreach ($folder in $sizes.Keys) {
    $path = "e:\bot\novel\app\src\main\res\$folder"
    if (!(Test-Path $path)) { New-Item -ItemType Directory -Force -Path $path | Out-Null }
    $size = $sizes[$folder]
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, $size, $size)
    $bmp.Save("$path\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save("$path\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}
$img.Dispose()
Write-Output "Icons resized successfully"
