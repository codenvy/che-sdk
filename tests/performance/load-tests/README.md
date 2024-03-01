# Overview
This script tests how well OpenShift environment can handle running simultaneously many of workspaces. It evaluates the performance of the system under test by checking the average results across all pods and identifying failures that occur during the testing process. 

## Prerequisites
What do you need to run those tests
- `kubectl` client installed
- Openshift cluster with running Openshift DevSpaces
- test user logged into DevSpaces Dashboard(this quaranies that user namespaces are created)

## Running load tests
1. Log in to Openshift cluster with Openshift DevSpaces or Eclipse Che deployed from terminal
2. Start `load-test.sh` script from `test/e2e/performance/load-tests`. Set number of started workspaces by -c parameter(like ./load-test.sh -c 5). Set timeout for waiting workspaces to start by -t parameter in seconds(like ./load-test.sh -t 240). 
3. This script uses local dewvorspace.yaml to starts workspaces.
4. As results there are average time of workspace starting and number of failed workspaces.


## Results and logs
If workspace failed to start, logs are saved in current directory.
