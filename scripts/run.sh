set -euo pipefail

mvn -q clean package

java -jar cli/target/apr-cli.jar "$@"
