# modsync-blocker
![buildbadge](https://img.shields.io/badge/build-passing-badge?style=flat) ![version](https://img.shields.io/badge/version-1.21.11-blue?style=flat) 

This is a mod that blocks ModSync requests, but still lets you play on servers with the plugin

**There is how it works:**

* The server sends a S2C packet -> mod intercepts the packet and sends fake C2S packets.
* It still lets you automatically log in, because the UUID is stored in mod's config.

**What will the moderator see?**
* He will see that you have absolutely no mods.
* If he will request a screenshot, he'll request a 1x1 black pixel instead of your actual screen.

**I'm in, how do i build?**

* Clone the repository:

      git clone https://github.com/g70xl0l/modsync-blocker.git
* Then, go into the folder:

      cd modsync-blocker
* Then, run the build itself:

      gradlew build

* Your mod will be at ./build/libs.


**What if I don't want to wait?**

* Then, just download the build from Releases.

_Good luck!_
