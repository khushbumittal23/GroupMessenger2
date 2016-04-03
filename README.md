# GroupMessenger2
An Android app that implements <b>ISIS algorithm</b> to provide <b>TOTAL and FIFO Ordering Guarantees</b>. All the messages sent by various avds are stored in their 
respective content providers. The app can be run on upto 5 avds as the port numbers are hardcoded. More avds can be supported by providing
more port numbers in the GroupMessengerActivity class. One avd failure is supported as well. The project has been tested on Linux environment only.

To run the app follow the instructions:</br>
	1) Download the project and copy it into Android Studio projects directory.</br>
	2) Download and save create_avd.py, run_avd.py, and set_redir.py in your home folder.</br>
	3) Open terminal and run the command: <b>python create_avd.py 5 your_Android_SDK_directory_Path</b></br> 
			In the middle of the script, it will ask “Do you wish to create a custom hardware profile [no]” multiple times. </br> 
			The script handles it automatically, so please do not enter anything to answer that question.</br>
			5 AVDs will be created by the above command.</br>
	4) Next you need to run the 5 avds by using the command: <b>python run_avd.py 5</b></br>
	5) After all the avds are successfully running, use the command to establish a connection between them: <b>python set_redir.py 10000</b></br>
	6) Open the project in Android Studio and build the project by using "Build APK" option from the menu.</br>
	7) Once the project is build, you can run the project directly from the Android Studio on the 5 opened avds</br>
	8) The project can also be tested by using the grader file. The grader output 10 will indicate that the app is working perfectly. </br>
	   To run the grader, save the groupmessenger2-grading.linux file and run the command: <b>groupmessenger2-grading.linux path_to_APK_file</b></br>
	9) Before you run the app through grader or from Android Studio, make sure that the avds are running the connection is established between them by using setdir.py command
