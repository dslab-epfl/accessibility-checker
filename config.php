<?php  // Moodle configuration file

unset($CFG);
global $CFG;
$CFG = new stdClass();

$CFG->dbtype    = 'mysqli';
$CFG->dblibrary = 'native';
$CFG->dbhost    = 'localhost';
$CFG->dbname    = 'moodle';
$CFG->dbuser    = 'root';
$CFG->dbpass    = '';
$CFG->prefix    = '';
$CFG->dboptions = array (
  'dbpersist' => 0,
  'dbport' => '',
  'dbsocket' => '',
);

$CFG->wwwroot   = 'http://localhost/moodle';
$CFG->dataroot  = 'C:\\wamp\\www\\moodle-data';
$CFG->admin     = 'admin';

$CFG->directorypermissions = 0777;

$CFG->admineditalways = true;
$CFG->filelifetime = 15;
$CFG->disableupdatenotifications = true;
$CFG->disableupdateautodeploy = true;

@error_reporting(E_ALL | E_STRICT);
@ini_set('display_errors', '1');
$CFG->debug = E_ALL &~ E_STRICT;
$CFG->dblogerror = true;
$CFG->debugdisplay = 1;
$CFG->cachejs = false;
//$CFG->noemailever = true;
$CFG->divertallemailsto = 'quentin.cosendey@epfl.ch';


require_once(dirname(__FILE__) . '/lib/setup.php');

// There is no php closing tag in this file,
// it is intentional because it prevents trailing whitespace problems!
