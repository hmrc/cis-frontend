#!/bin/bash

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /$domain$/$className;format="decap"$                        controllers.$domain$.$className$Controller.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /$domain$/$className;format="decap"$                        controllers.$domain$.$className$Controller.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /$domain$/change-$className;format="decap"$                  controllers.$domain$.$className$Controller.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /$domain$/change-$className;format="decap"$                  controllers.$domain$.$className$Controller.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.title = $className;format="decap"$" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.heading = $className;format="decap"$" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.checkYourAnswersLabel = $className;format="decap"$" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.error.required = Enter $className;format="decap"$" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.error.length = $className$ must be $maxLength$ characters or less" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.change.hidden = $className$" >> ../conf/messages.en

echo "Migration $className;format="snake"$ completed"
