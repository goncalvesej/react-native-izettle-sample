import React, {useEffect, useState} from 'react';

import {NativeEventEmitter, SafeAreaView} from 'react-native';

import {IZettleModule} from './modules/';

import {
  Text,
  Container,
  BottomButtonContainer,
  Content,
  DefaultButton,
  DefaultButtonWhite,
  BTNText,
} from './styles';
const AUTH_STATUS_CHANGE_EVENT = 'iZettleUserAuthStatusChanged';

const MachineIZettle = () => {
  const [logged, setLogged] = useState(false);
  const [loggedUser, setUser] = useState<any>(null);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [pending, setPending] = useState(false);
  useEffect(() => {
    IZettleModule.initModule();
    const eventEmitter = new NativeEventEmitter(IZettleModule);
    const eventListener = eventEmitter.addListener(
      AUTH_STATUS_CHANGE_EVENT,
      ({status, user}) => {
        setLogged(status);
        setUser(user);
      },
    );

    return () => {
      eventListener.remove();
    };
  }, []);

  const login = () => {
    IZettleModule.login();
  };

  const logout = () => {
    IZettleModule.logout();
  };
  return (
    <SafeAreaView style={{flex: 1, paddingVertical: 60}}>
      <Container>
        <Content>
          {logged && loggedUser && (
            <>
              <Text>{loggedUser.publicName}</Text>
              <Text>{loggedUser.timeZone}</Text>
              <Text>{loggedUser.userId}</Text>
              <Text>{loggedUser.country}</Text>
              <Text>{loggedUser.currency}</Text>
            </>
          )}
        </Content>
        <BottomButtonContainer>
          {logged ? (
            <DefaultButtonWhite onPress={logout}>
              <BTNText>LOGOUT</BTNText>
            </DefaultButtonWhite>
          ) : (
            <DefaultButton onPress={login}>
              <BTNText>LOGIN</BTNText>
            </DefaultButton>
          )}
        </BottomButtonContainer>
      </Container>
    </SafeAreaView>
  );
};

export default MachineIZettle;
