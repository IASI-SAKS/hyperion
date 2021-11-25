#!/usr/bin/env python

import os
import subprocess
import sys
import json

hyperion_jar = "./target/hyperion-shaded-1.0-SNAPSHOT.jar"
sut_path = "../full-teaching-master/"

metrics = ["nonemptyIntersection", "nonemptySubset", "nonemptyEqSet"]
domains = ["invokes", "endpoint"]
similarity_thresholds = [0.3, 0.6, 0.9]
repetitions = 10

# Make SWI Prolog happy for running
my_env = {**os.environ,
          "SWI_HOME_DIR":"/usr/lib/swipl/",
          "CLASSPATH":"/usr/lib/swipl/lib/jpl.jar:$CLASSPATH",
          "LD_LIBRARY_PATH":"/usr/lib/swipl/lib/x86_64-linux/",
          "LD_PRELOAD":"/usr/lib/swipl/lib/x86_64-linux/libswipl.so",
         }

# Prepare folders
def prepare_folders():
    print("****** PREPARING FOLDERS *******")
    for metric in metrics:
        for domain in domains:
            for threshold in similarity_thresholds:
                for rep in range(repetitions):
                    os.makedirs("experiment/"+metric+"/"+domain+"-"+str(threshold)+"/"+str(rep)+"/", exist_ok=True)

# Run Symbolic Execution
def run_symbolic():
    print("****** SYMBOLIC EXECUTION *******")
    conf = (
            "{"
              "\"sut\": [\""+sut_path+"/target/classes/\"],"
              "\"testPrograms\": [\""+sut_path+"/target/test-classes/\"],"
              "\"includeTest\": [],"
              "\"excludeTest\": [],"
              "\"additionalClasspath\": [],"
              "\"excludeTracedPackages\": ["
                "\"it/cnr/saks/hyperion\","
                "\"java/\","
                "\"sun/\","
                "\"com/google/gson\""
              "],"
              "\"depth\": 100,"
              "\"testProgramsList\": \"allTPs.json\""
            "}"
           )
    with open("conf.json",'w',encoding = 'utf-8') as f:
        f.write(conf)
        f.close()
        process = subprocess.Popen("java -jar " + hyperion_jar + " --analyze conf.json",
                shell=True, stdout=sys.stdout, stderr=subprocess.STDOUT,
                env = my_env)
        process.wait()
    
# Generate similarity data
def generate_similarity():
     print("****** COMPUTING SIMILARITY *******")
     for metric in metrics:
         for domain in domains:
             conf = ("{"
                     "\"invokes\": ["
                     "\"./src/test/resources/sose/inspection-invokes.pl\""
                     "],"
                     "\"regex\": \"./src/test/resources/sose/URI-regex-list.pl\","
                     "\"metric\": \"" + metric + "\","
                     "\"outputFile\": \"experiment/similarTPs-" + metric + "-" + domain +".json\","
                     "\"domain\": \"" + domain + "\""
                     "}")
             with open("conf.json",'w',encoding = 'utf-8') as f:
                 f.write(conf)
                 f.close()
                 process = subprocess.Popen("java -jar " + hyperion_jar + " --extract-similarity conf.json",
                                            shell=True, stdout=sys.stdout, stderr=subprocess.STDOUT,
                                            env = my_env)
                 process.wait()
    
# Generate test groups
def test_groups():
     print("****** GENERATING TEST GROUPS *******")
     for metric in metrics:
         for domain in domains:
             for threshold in similarity_thresholds:
                 for rep in range(repetitions):
                     conf = ("{"
                             "\"similarityFile\": \"experiment/similarTPs-" + metric + "-" + domain +".json\","
                             "\"allTestProgramsFile\": \"allTPs.json\","
                             "\"policy\": \"policy 1\","
                             "\"threshold\": 0.7,"
                             "\"outputFile\": \"experiment/"+metric+"/"+domain+"-"+str(threshold)+"/"+str(rep)+"/testGroup.json\""
                             "}")
                     with open("conf.json",'w',encoding = 'utf-8') as f:
                         f.write(conf)
                         f.close()
                         process = subprocess.Popen("java -jar " + hyperion_jar + " --group-similar-tests conf.json",
                                                    shell=True, stdout=sys.stdout, stderr=subprocess.STDOUT,
                                                    env = my_env)
                         process.wait()


# Run the test suite to compute coverage
def get_coverage():
    print("****** COMPUTING COVERAGE *******")
    for metric in metrics:
       for domain in domains:
           for threshold in similarity_thresholds:
               for rep in range(repetitions):
                   f = open("experiment/"+metric+"/"+domain+"-"+str(threshold)+"/"+str(rep)+"/testGroup.json")
                   similar = json.load(f)
                   f.close()
                   test_list = ""
                   for test in similar['include']:
                       test_list += test.replace(":", "#") + ",\n"
                   with open(sut_path + "pom.xml",'w',encoding = 'utf-8') as dest:
                       with open(sut_path + "pom.xml.template",'r',encoding = 'utf-8') as tpl:
                           for line in tpl:
                               if "#include \"test-list\"" in line:
                                   dest.write(test_list)
                               else:
                                   dest.write(line)
                   process = subprocess.Popen("./src/test/resources/journal/coverage.sh experiment/"+metric+"/"+domain+"-"+str(threshold)+"/"+str(rep)+"/",
                                          shell=True, stdout=sys.stdout, stderr=subprocess.STDOUT,
                                          env = my_env)
                   process.wait()

prepare_folders()
#run_symbolic()
generate_similarity()
#test_groups()
#get_coverage()

