# 서론

<h3>목적: FTK의 PRTK 툴을 분석하여, 동글 통신 과정을 분석하고자 함.</h3>
<h3>사용 Tool: JD-GUI</h3>
<h3>PRTK 버전: 6.4</h3>

<br />
<br />

# DNAP(DNA Protocol) 통신 과정

## 포트 목록

![포트목록](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/PRTK_portlist.png)

<br />

## 목차
1. [릴리즈 정보 교환](#1-릴리즈-정보-교환)

2. [통신 암호화 객체](#2-암호화-통신-객체)
    - [2 - 1. startSessions](#2-1-startsession)
    - [2 - 2. negotiateEncryption](#2-2-negotiateencryption)
    - [2 - 3. initializeRSA](#2-3-initializersa)
    - [2 - 4. handshake](#2-4-handshake)
    - [2 - 5. exchangeSessionKey](#2-5-exchangesessionkey)

3. [Invoke](#3-invoke)

4. [Supervisor](#4-supervisor)

5. [Worker](#5-worker)

<br />
<br />

## __1.__ 릴리즈 정보 교환
---
![분석화면1-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze1-1.png)

`Communicator.addCommunicator`를 통해 동글 서버와 통신하고 있으며, init을 통해 통신 암호화 여부를 결정하고 있다.

![분석화면2-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze2-1.png)

![분석화면2-2](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze2-2.png)

![분석화면2-3](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze2-3.png)

`exchangeRelease` 함수는 이후 분석하겠지만, 간단하게만 설명하자면, __서버와 클라이언트 간 릴리즈 버전을 교환__ 하는 함수다. 이후, TcpNoDelay를 설정하고, Communicator 객체를 생성해 반환하고 있다.

![분석화면3-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze3-1.png)

![분석화면3-2](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze3-2.png)

bytes형으로 릴리즈 버전을 통신하며, 종단에서 long으로 변환한다. 또한, 릴리즈 버전은 native 함수를 통해 가져오고 있음을 확인할 수 있다.

<br />
<br />

## __2.__ 암호화 통신 객체
---
![분석화면4-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze4-1.png)

통신 객체에서 사용하는 변수들을 초기화하는 함수, 주목해야할 부분은 `mProtocol.startSession`이다.

<br />
<br />

### __2-1.__ startSession
---
![분석화면5-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze5-1.png)

`negotiateEncryption`을 통과한 이후 RSA 초기화, 핸드쉐이킹, 세션키 교환을 진행한다. 패킷 암/복호화는 AES 방식으로, `exchangeSessionKey`를 통해 서로 교환한 세션키를 사용한다. 또한, 해당 함수의 끝에서 서버와 클라이언트의 GUI 환경 정보를 교환하고 있다. 

다음 장에서, 세션 설정에 사용되는 함수들을 자세하게 알아본다.

<br />
<br />

### __2-2.__ negotiateEncryption
---
![분석화면6-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze6-1.png)

`negotiateEncryption`에서는 n(Modulus), e(Exponent) 값의 존재 여부에 따라 통신 암호화를 할지 결정한다. 만약 해당 값들이 존재한다면,
서버와 클라이언트에서 서로 특정 값(1)을 주고 받는다.

<br />
<br />

### __2-3.__ initializeRSA
___
![분석화면7-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze7-1.png)

![분석화면7-2](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze7-2.png)

`negotiateEncryption`의 값이 true이면, `initializeRSA`를 통해 공개키 암호화 통신을 위한 초기화 작업을 한다. 서버와 공개키를 서로 교환하는 것 같은데,
서버에 아마 `dna3.Worker.getPublicKey` 메소드가 존재하여, 해당 메소드로부터 키를 받아오는 것 같다.

<br />

![분석화면7-3](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze7-3.png)

결론부터 말하자면, `dna3.Worker.getPublicKey` 메소드는 Engine.jar 파일 내에 존재한다. 자세한 내용은 [3장 Invoke](#3-invoke)를 참고. 일단, 해당 메소드를 분석해보면, SUPERVISOR 설정 파일로부터 Modulus와 PublicExponent를 가져와 클라이언트로 전달함으로써, 키 교환을 과정을 거친다. 

키 교환이 끝나면, RSA 암/복호화 객체를 생성한다.

<br />
<br />

### __2-4.__ handshake
---
![분석화면8-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze8-1.png)

RSA 통신 초기화가 끝나고, 서버와 클라이언트는 핸드쉐이킹 과정을 거쳐 키가 제대로 교환이 되었는지 확인한다.

<br />
<br />

### __2-5.__ exchangeSessionKey
___
![분석화면9-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze9-1.png)

난수로 임의의 8바이트 키(lowKey)를 생성하고, 서버로 전송한다. 그리고, 서버로부터 똑같이 8바이트 크기의 hiKey를 읽어와 리틀 엔디안(?) 방식으로 이어붙여 16바이트의 AES 암호화 키를 생성한다.

<br />
<br />

## __3.__ Invoke
---

![분석화면10-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze10-1.png)

xml 형태로, CLASS, METHOD, PARAMETERS 정보를 전달해 결과값을 받고 있다. ___~~아직 동글쪽을 분석해보진 않았지만, xml 형식만 봐서는 원격코드 실행도 가능할 같다.~~___

일단, 위의 코드에서는 __`dna3.Worker.setAuthority`__ 메소드를 전달하고 있어, 같은 `PRTK.jar` 파일 내 `dna3.Worker.setAuthority` 메소드를 찾으려고 했으나, 찾을 수 없었다.

해당 메소드는 `Engine.jar` 파일 내에 위치한 것으로 확인됐다.

<br />

![분석화면11-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze11-1.png)
___Engine.jar 내 dna3.Worker.setAuthority___

<br />

![분석화면12-1](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze12-1.png)

![분석화면12-2](https://raw.githubusercontent.com/CentaProxima/FTK-License-Server-Simulator/main/resources/analyze12-2.png)

PRTK class의 main메소드에서 engine.jar의 `dna3.Supervisor`와 `dna3.Worker`를 각각 구동하고 있음을 알 수 있으며, Supervisor 작동과정과 Worker 작동 과정은 아래와 같다.

<br />
<br />

## __4.__  Supervisor
---

<br />
<br />

## __5.__ Worker
---

<br />
<br />