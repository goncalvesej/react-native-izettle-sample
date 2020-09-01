import styled from 'styled-components/native';
import {StyleSheet} from 'react-native';

const styles = StyleSheet.create({
  img: {
    flex: 1,
    minWidth: 200,
  },
});

const Container = styled.View`
  background-color: #fff;
  flex: 1;
`;
const Title = styled.Text`
  font-family: Roboto_Regular;
  color: #fff;
  font-size: 26px;
  margin-top: 15px;
`;

const ImageContainer = styled.View`
  background-color: #0073cd;
  flex: 1;
  align-items: center;
  justify-content: center;
`;

const ButonContainer = styled.View`
  align-items: stretch;
  align-self: stretch;
  margin-vertical: 15px;
`;

const BottomButtonContainer = styled.View`
  align-items: stretch;
  align-self: stretch;
  background-color: #fff;
  padding-vertical: 15px;
  padding-horizontal: 15px;
  background-color: #eee;
`;

const Content = styled.View`
  flex: 1;
  flex-direction: column;
  padding-horizontal: 16px;
`;

const Text = styled.Text`
  font-family: Robot-Regular;
  font-size: 16px;
  color: #ccc;
`;

const DefaultButton = styled.TouchableOpacity`
  border-radius: 2px;
  background-color: #ffc800;
  flex-direction: row;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 60px;
  padding: 15px;
  padding-vertical: 15px;
`;

const DefaultButtonWhite = styled.TouchableOpacity`
  border-radius: 2px;
  background-color: #fff;
  flex-direction: row;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 60px;
  padding-horizontal: 15px;
  padding-vertical: 15px;
`;

const BTNText = styled.Text`
  font-size: 30px;
  color: #333;
`;

export {
  styles,
  Text,
  ButonContainer,
  Container,
  BottomButtonContainer,
  Content,
  ImageContainer,
  Title,
  DefaultButton,
  DefaultButtonWhite,
  BTNText,
};
