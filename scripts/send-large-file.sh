#!/bin/sh

# Generate large XML file with generate-large-file.sh
# install the uuid CLI with:
# sudo apt-get install uuid

set -e

MESSAGEID=`uuid`

TOKEN=`curl 'http://localhost:8081/jme-message-exchange-auth-scs/oauth2/token' \
  -H 'Accept: application/json, text/plain, */*' \
  -H 'Accept-Language: en-US,en;q=0.9,de;q=0.8' \
  -H 'Authorization: Basic aW50ZXJuYWwtY2xpZW50OnNlY3JldA==' \
  -H 'Connection: keep-alive' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'Origin: http://localhost:8080' \
  -H 'Referer: http://localhost:8080/' \
  -H 'Sec-Fetch-Dest: empty' \
  -H 'Sec-Fetch-Mode: cors' \
  -H 'Sec-Fetch-Site: same-site' \
  -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36' \
  -H 'X-Requested-With: XMLHttpRequest' \
  -H 'sec-ch-ua: "Google Chrome";v="119", "Chromium";v="119", "Not?A_Brand";v="24"' \
  -H 'sec-ch-ua-mobile: ?0' \
  -H 'sec-ch-ua-platform: "Linux"' \
  --data-raw 'grant_type=client_credentials' \
  --compressed --silent | jq -r .access_token`

curl -v -X 'PUT' \
  "http://localhost:8080/jme-message-exchange-service/api/internal/v2/messages/$MESSAGEID?topicName=t&groupId=g" \
  -H 'accept: */*' \
  -H 'bpId: 123' \
  -H 'messageType: t' \
  -H 'partnerTopic: t' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/xml' \
  --data-binary @large-file.xml
