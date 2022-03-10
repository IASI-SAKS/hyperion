#!/bin/bash

PWD=$(pwd)
DESTINATION="$PWD/$1"
echo $DESTINATION

if [ -f "$DESTINATION/jacoco.exec" ]; then
    echo "Already run, skipping."
    exit 0
fi

cd ../train-ticket
find . -name "jacoco*" -exec unlink {} \;

mvn test -DfailIfNoTests=false

JACOCO_EXEC=$(find . -name "jacoco.exec"  | tr "\n" " ")
java -jar ../jacoco-0.8.7/lib/jacococli.jar merge $JACOCO_EXEC --destfile $DESTINATION/jacoco.exec
java -jar ../jacoco-0.8.7/lib/jacococli.jar report $DESTINATION/jacoco.exec --classfiles ts-admin-basic-info-service/target/classes/ --classfiles ts-admin-order-service/target/classes/ --classfiles ts-admin-route-service/target/classes/ --classfiles ts-admin-travel-service/target/classes/ --classfiles ts-admin-user-service/target/classes/ --classfiles ts-assurance-service/target/classes/ --classfiles ts-auth-service/target/classes/ --classfiles ts-basic-service/target/classes/ --classfiles ts-cancel-service/target/classes/ --classfiles ts-common/target/classes/ --classfiles ts-config-service/target/classes/ --classfiles ts-consign-price-service/target/classes/ --classfiles ts-consign-service/target/classes/ --classfiles ts-contacts-service/target/classes/ --classfiles ts-execute-service/target/classes/ --classfiles ts-food-map-service/target/classes/ --classfiles ts-food-service/target/classes/ --classfiles ts-inside-payment-service/target/classes/ --classfiles ts-notification-service/target/classes/ --classfiles ts-order-other-service/target/classes/ --classfiles ts-order-service/target/classes/ --classfiles ts-payment-service/target/classes/ --classfiles ts-preserve-other-service/target/classes/ --classfiles ts-preserve-service/target/classes/ --classfiles ts-price-service/target/classes/ --classfiles ts-rebook-service/target/classes/ --classfiles ts-route-plan-service/target/classes/ --classfiles ts-route-service/target/classes/ --classfiles ts-seat-service/target/classes/ --classfiles ts-security-service/target/classes/ --classfiles ts-ticketinfo-service/target/classes/ --classfiles ts-train-service/target/classes/ --classfiles ts-travel2-service/target/classes/ --classfiles ts-travel-plan-service/target/classes/ --classfiles ts-travel-service/target/classes/ --classfiles ts-user-service/target/classes/ --classfiles ts-verification-code-service/target/classes/ --classfiles ts-station-service/target/classes/ --html $DESTINATION