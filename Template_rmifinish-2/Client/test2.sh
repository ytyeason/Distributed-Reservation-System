# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

java -Djava.security.policy=java.policy -cp ../Server/RMIInterface.jar:. Client.testClient2 $1 $2
