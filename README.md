# SnapScanHybridPlugin
SnapScanHybridPlugin for cordova &amp;&amp; ionic


1/ Create a ionic project
  $ ionic start myApp sidemenu
 
2/ Add android platform
  $ ionic platform add android
  
3/ add android support package
  $ ionic plugin add https://github.com/MobileChromeApps/cordova-plugin-android-support-v4.git
  
3/ add our snapscan test plugin
  $ ionic plugin add <URL_FOLDER_PLUGIN>

4/ Conect an Android Device

5/ run app with ionic CLI (this two commands is the same)
  
  $ ionic emulate ios --livereload --consolelogs --serverlogs
  $ ionic run android -l -c -s
  


6/ If want to use crosswalk (is an option)
  $ ionic browser add crosswalk
