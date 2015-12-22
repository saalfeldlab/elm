# elm
Electron-Light Microscopy tools for Janelia

This project enables visualization of light microscopy in the space of a particular version of 
FAFB (internal to Janelia only).  Any images in the space of the fly light nc82 template (size: `1450 x 725 x 436` ) can be visualized.  
The transformation was generated manually using [bigwarp](http://fiji.sc/BigWarp) which in turn is built upon
[BigDataViewer](http://fiji.sc/BigDataViewer).

Pressing the 'K' key will open the FAFB stack in [CATMAID](http://catmaid.readthedocs.org/en/stable/) at the 
location displayed in the current window.

## Installation
Two alternatives:

1. Using GIT:
  
  Clone this repository into your fiji plugins folder.
  
  ```bash
  cd <your fiji folder>/plugins
  git clone https://github.com/saalfeldlab/elm.git
  ```
2. Download and copy:
  
  Download the [zipped archive](https://github.com/saalfeldlab/elm/archive/master.zip) and extract into
  
  ```bash
  <your fiji folder>/plugins/elm/
  ```

## Running
* After installation, the script can be accessed from the Fiji menu by:  
  * Plugins > elm > ELM
* Use the dialog to select an image to warp into FAFB space
  * Note: it must be coregistered to the template nc82 stack
  * If "Auto discover" is selected, ELM will open the selected image and, if the selected image has only one channel, will search for other channels:
      1. in the same folder,
      2. with the same extension, and
      3. the same size as the selected image.
    
    This is the way to open multi-channel exports that come out as separate files.
