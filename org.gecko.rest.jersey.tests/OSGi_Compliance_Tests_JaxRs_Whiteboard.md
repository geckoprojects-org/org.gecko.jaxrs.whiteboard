# OSGi Compliance Tests for JaxRs Whiteboard

+ **`ApplicationLifecycleTestCase`**

  |                  Test Name                   | Passed? |                        Failure Reason                        |
  | :------------------------------------------: | :-----: | :----------------------------------------------------------: |
  |        `testSimpleSingletonResource`         |   YES   |                                                              |
  |        `testSimplePrototypeResource`         |   YES   |                                                              |
  |            `testApplicationBase`             |   YES   |                                                              |
  |       `testApplicationPathAnnotation`        |   YES   |                                                              |
  |     `testApplicationWhiteboardResource`      |   YES   |                                                              |
  |     `testApplicationIsolationExtensions`     |   YES   |                                                              |
  |    `testApplicationIsolationExtensions2`     |   YES   |                                                              |
  | `testApplicationProvidedExtensionDependency` |   YES   |                                                              |
  |     `testApplicationExtensionDependency`     |   YES   |                                                              |
  |         `testSimpleWhiteboardTarget`         |   YES   |                                                              |
  |     `testApplicationIsolationContainer`      | **NO**  | Same session reused in OSGi tests </br> and always new session in ours ?? |
  |           `testShadowDefaultPath`            | **NO**  |               Conflicts between test and Specs               |
  |         `testMoveDefaultApplication`         |   YES   |                                                              |
  |        `testApplicationServiceProps`         |   YES   |                                                              |
  |    `testApplicationServicePropsInFeature`    |   YES   |                                                              |



+ **`CapabilityTestCase`**

  |                      Test Name                       | Passed? | Failure Reason |
  | :--------------------------------------------------: | :-----: | :------------: |
  |      `testJaxRsServiceRuntimeServiceCapability`      |   YES   |                |
  |      `testJaxRsClientBuilderServiceCapability`       |   YES   |                |
  |     `testSseEventSourceFactoryServiceCapability`     |   YES   |                |
  | `testJaxRsServiceWhiteboardImplementationCapability` |   YES   |                |
  |            `testJaxRsContractCapability`             |   YES   |                |




+ **`ClientTestCase`**

  |          Test Name          | Passed? |      Failure Reason       |
  | :-------------------------: | :-----: | :-----------------------: |
  |  `testJaxRsClientService`   |  MAYBE  | Our equivalent test works |
  | `testJaxRsPromiseRxInvoker` |  MAYBE  | Our equivalent test works |

+ **`ExtensionLifecylceTestCase`**

  |               Test Name                | Passed? |                        Failure Reason                        |
  | :------------------------------------: | :-----: | :----------------------------------------------------------: |
  |         `testSimpleExtension`          |   YES   |                                                              |
  |        `testNameBoundExtension`        |   YES   |                                                              |
  |        `testExtensionOrdering`         | **NO**  | When two extensions are advertising the same </br> interface they are not properly registered |
  |    `testResourceRequiresExtension`     |   YES   |                                                              |
  |    `testExtensionRequiresExtension`    |   YES   |                                                              |
  |      `testSimpleWhiteboardTarget`      |   YES   |                                                              |
  |         `testFeatureExtension`         |   YES   |                                                              |
  |     `testDynamicFeatureExtention`      |   YES   |                                                              |
  |    `testReaderInterceptorExtension`    |  MAYBE  | Failing due to HK2 shut down due to EchoResource </br> check after update |
  | `testContainerRequestFilterExtension`  |  MAYBE  | Failing due to HK2 shut down due to EchoResource </br> check after update |
  | `testContainerResponseFilterExtension` |  MAYBE  | Failing due to HK2 shut down due to EchoResource </br> check after update |
  |    `testMessageBodyReaderExtension`    |  MAYBE  | Failing due to HK2 shut down due to EchoResource </br> check after update |
  |    `testMessageBodyWriterExtension`    |  MAYBE  | Failing due to HK2 shut down due to EchoResource </br> check after update |
  | `testParamConverterProviderExtension`  |  MAYBE  | Failing due to HK2 shut down due to EchoResource </br> check after update |
  |     `testExceptionMapperExtension`     |   YES   |                                                              |
  | `testExtensionWhenApplicationChanges`  |   NO    |                              ??                              |



+ **`JaxRSServiceRuntimeTestCase`**

  |            Test Name             | Passed? | Failure Reason |
  | :------------------------------: | :-----: | :------------: |
  |   `testWhiteboardResourceDTO`    |   YES   |                |
  |   `testWhiteboardExtensionDTO`   |   YES   |                |
  |       `testNameBoundDTOs`        |   YES   |                |
  |  `testWhiteboardApplicationDTO`  |   YES   |                |
  | `testResourcesWithClashingNames` |   YES   |                |
  |      `testMissingExtension`      |   YES   |                |
  |    `testMissingApplications`     |   YES   |                |
  |      `testInvalidProperty`       |   YES   |                |
  |    `testInvalidExtensionType`    |   YES   |                |
  |     `testUngettableService`      |   YES   |                |
  |    `testApplicationShadowing`    |   YES   |                |

  

+ **`ResourceLifecycleTestCase`**

  |                   Test Name                   | Passed? |    Failure Reason    |
  | :-------------------------------------------: | :-----: | :------------------: |
  |         `testSimpleSingletonResource`         |   YES   |                      |
  |         `testSimplePrototypeResource`         |   YES   |                      |
  |          `testContextFieldInjection`          | **NO**  | Check after updating |
  |         `testAsyncPrototypeResource`          |   YES   |                      |
  |         `testSimpleWhiteboardTarget`          |   YES   |                      |
  | `testSingletonResourceWhenApplicationChanges` |   YES   |                      |

  

  

+ **`SSETestCase`**

  |            Test Name             | Passed? |                        Failure Reason                        |
  | :------------------------------: | :-----: | :----------------------------------------------------------: |
  | `testJaxRsSseEventSourceFactory` | **NO**  | ava.lang.NoClassDefFoundError: sun/misc/Unsafe </br> Check after updating |

  

  

+ **`SignatureTestCase`**

  |    Test Name     | Passed? | Failure Reason |
  | :--------------: | :-----: | :------------: |
  | `testSignatures` |   YES   |                |

  

57 tests

9 to check after updating dependency

1 in conflicts with spec

