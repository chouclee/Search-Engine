import os.path,subprocess
from subprocess import STDOUT,PIPE
import requests

def execute_java(java_file, input):
    java_class,ext = os.path.splitext(java_file)
    cmd = ['java','-cp', '.:lucene-4.3.0/*', java_class, input]
    proc = subprocess.Popen(cmd)
    stdout,stderr = proc.communicate()



fileIn = '/home/happyuser/HW1-queries-UB.teIn'
#os.remove(fileIn)

#execute_java('QryEval.class', '/home/happyuser/parameter.txt')

url = 'http://boston.lti.cs.cmu.edu/classes/11-642/HW/HTS/tes.cgi'
files = {'logtype':'Detailed', 'infile' : ('HW1-queries-UB.teIn', open(fileIn, 'rb')), 'hwid': 'HW4'}

#r = requests.post(url, files=files, auth=('zhouchel', 'ZTE2MzE5'))
print os.popen('''
	perl -e '
		use LWP::Simple;
		my $fileIn = "/home/happyuser/HW1-queries-UB.teIn";
		my $url = "http://boston.lti.cs.cmu.edu/classes/11-642/HW/HTS/tes.cgi";
		my $ua = LWP::UserAgent->new();
   		$ua->credentials("boston.lti.cs.cmu.edu:80", "HTS", "zhouchel", "ZTE2MzE5");
		my $result = $ua->post($url,
		       Content_Type => "form-data",
		       Content => [ logtype => "Detailed", infile => [$fileIn],	hwid => "HW4"]);
		my $result = $result->as_string;
   		$result =~ s/<br>/\n/g;
		print $result;'
	''').read()

#print r.text.replace('<br>', '\n')
#print fetchUrl()
