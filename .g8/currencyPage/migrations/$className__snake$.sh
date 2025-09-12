#!/bin/bash

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /$domain$/$className;format="decap"$                  controllers.$domain$.$className$Controller.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /$domain$/$className;format="decap"$                  controllers.$domain$.$className$Controller.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /$domain$/change-$className;format="decap"$                        controllers.$domain$.$className$Controller.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /$domain$/change-$className;format="decap"$                        controllers.$domain$.$className$Controller.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.title = $className$" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.heading = $className$" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.checkYourAnswersLabel = $className$" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.error.nonNumeric = Enter your $className;format="decap"$ using numbers and a decimal point" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.error.required = Enter your $className;format="decap"$" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.error.invalidNumeric = Enter your $className;format="decap"$ using up to two decimal places" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.error.aboveMaximum = $className$ must be {0} or less" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.error.belowMinimum = $className$ must be {0} or more" >> ../conf/messages.en
echo "$domain$.$className;format="decap"$.change.hidden = $className$" >> ../conf/messages.en

echo "Migration $className;format="snake"$ completed"
