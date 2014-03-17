<?php /**
This plugin is used to check accessibility of PDF documents uploaded by teachers.
When a file is uploaded, it is checked for accessibility
Then, in case of potential problems, a report is displayed in the browser
*/

function course_file_uploaded ($cc) {
$cc->msg = 'Hello, world! '.$cc->file;
return true;
}

