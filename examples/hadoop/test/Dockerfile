FROM flokkr/hadoop
RUN sudo apk add --update --no-cache py2-pip
RUN sudo pip install --upgrade pip
RUN sudo pip install robotframework
ADD test.robot /tmp/tests/test.robot
ADD tester.sh /tmp/tester.sh
CMD /tmp/tester.sh

