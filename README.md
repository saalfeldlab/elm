# elm
Electron-Light Microscopy tools for Janelia

This project enables visualization of light microscopy in the space of a particular version of 
FAFB (internal to Janelia only).  Any images in the space of the fly light nc82 template (size: `1450 x 725 x 436` ) can be visualized.  
The transformation was generated manually using [bigwarp](http://fiji.sc/BigWarp) which in turn is built upon
[BigDataViewer](http://fiji.sc/BigDataViewer).

See also [elmr](https://github.com/jefferis/elmr), for interaction between the CATMAID web application and the R Neuroanatomy Toolbox package. 


## Announcements
* ``2016-Dec-16`` - ELM now uses FAFB v13 by default. 
* ``2016-Apr-01`` - ELM now uses FAFB v12 by default. 
* ``2016-Feb-22`` - Landmark improvements thanks to Davi Bock.
* ``2016-Feb-09`` - ELM now uses FAFB v11 by default.

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
  <your fiji folder>/plugins/elm-master/
  ```
  or
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

## Hotkeys
The 'K' key will open the FAFB stack in [CATMAID](http://catmaid.readthedocs.org/en/stable/) at the 
location displayed in the current window.

The 'L' key (press and hold ) will display the name of the label under the mouse cursor if compartment labels are loaded.

## Compartment labels
As of 20 January 2016, ELM includes pointers to compartment labels maps.
Label maps are not loaded by default, but one can be selected with the radio buttons:
* DPX - a label map internal to Janelia.
* VFB - labels from the [Virtual fly brain](https://github.com/VirtualFlyBrain/DrosAdultBRAINdomains)
  * These were manually registered to the high-resolution template by John Bogovic (improvements welcome).

If a label map is selected, it is visible by default.  This can be changed using the *Visibility and Grouping* dialog (F6 (moving) and F7 (target)).  See details on the [big data viewer page](http://fiji.sc/BigDataViewer#Displaying_Multiple_Sources).
