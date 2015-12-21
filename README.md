# elm
Electron-Light Microscopy tools for Janelia

This project enables visualization of light microscopy in the space of a particular version of 
FAFB (internal to Janelia only).  Any images in the space of the fly light nc82 template can be visualized.  
The transformation was generated manually using [bigwarp](http://fiji.sc/BigWarp) which in turn is built upon
[BigDataViewer](http://fiji.sc/BigDataViewer).

## Installation
Clone this repository into your fiji plugins folder.
```bash
cd <your fiji folder>/plugins
git clone https://github.com/saalfeldlab/elm.git
```

## Running
* After installation, the script can be accessed from the Fiji menu by:  
  * Plugins > elm > ELM_Navigator
* Use the dialog to select an image to warp into FAFB space
  * Note: it must be coregistered to the template nc82 stack
  * If "Auto discover" is selected, ELM will open the selected image and will search for images:
      1. In the same folder
      2. With the same extension, and
      3. the same size as the selected image
