# JaxRs Gecko Tests

+ **ApplicationIsolationTest**

  |            **Test Name**            | **Passed?** |     **Failure Reason**     |
  | :---------------------------------: | :---------: | :------------------------: |
  | `testApplicationIsolationContainer` |   **NO**    | always new session in ours |

  

+ **JaxRsWhiteboardExtensionTests**

  |           **Test Name**            | **Passed?** |                      **Failure Reason**                      |
  | :--------------------------------: | :---------: | :----------------------------------------------------------: |
  |     `testExtensionNoContracts`     |   **YES**   |                                                              |
  |      `testExtensionContracts`      |   **YES**   |                                                              |
  |      `testExtensionSelectOK`       |   **YES**   |                                                              |
  |     `testWrongExtensionSelect`     |   **YES**   |                                                              |
  | `testWrongResourceExtensionSelect` |   **YES**   |                                                              |
  |   `testAppWrongExtensionSelect`    |   **YES**   |                                                              |
  |      `testAppExtensionSelect`      |   **YES**   |                                                              |
  |      `testWBExtensionSelect`       |   **YES**   |                                                              |
  |    `testWrongWBExtensionSelect`    |   **YES**   |                                                              |
  |      `testExtensionSameName`       |   **YES**   |                                                              |
  |     `testExtensionDependency`      |   **YES**   |                                                              |
  |    `testAppExtensionDependency`    |   **YES**   |                                                              |
  |      `testExtensionOrdering`       |   **NO**    | When two extensions are advertising the same </br> interface they are not properly registered. </br> They both seem active but only the last one is called. (151.5.2) |
  |       `testWhiteboardTarget`       |   **YES**   |                                                              |
  |  `testDefaultAppWhiteboardTarget`  |   **YES**   |                                                              |
  |   `testRemoveSingletonExtension`   |   **NO**    |           Extension is not really unregistered ??            |

  

+ **JaxRsResourceLifecycleTests**

  |         **Test Name**         | **Passed?** |                      **Failure Reason**                      |
  | :---------------------------: | :---------: | :----------------------------------------------------------: |
  |      `testContextInject`      |   **NO**    | Header not injected as field with @Context annotation (151.4.2.1). </br> When using @Context with method param it works! |
  | `testDefaultAppContextInject` |   **YES**   |              here we are not using prototypes!!              |

+ **JaxRsWhiteboardApplicationLifecycleTests**

  |                        **Test Name**                         | **Passed?** | **Failure Reason** |
  | :----------------------------------------------------------: | :---------: | :----------------: |
  | `testWhiteboardComponentAnnotatedLegacyApplicationPathChange` |   **YES**   |                    |
  |                `testWhiteboardResourceChange`                |   **YES**   |                    |
  |                 `testMoveDefaultApplication`                 |   **YES**   |                    |
  |              `testChangeDefaultApplicationRes`               |   **YES**   |                    |
  |              `testChangeDefaultApplicationExt`               |   **YES**   |                    |
  |                    `testAppSameBasePath`                     |   **YES**   |                    |
  |                      `testAppSameName`                       |   **YES**   |                    |

+ **JaxRsWhiteboardChangeCountTest**

  |                        **Test Name**                         | **Passed?** |  **Failure Reason**   |
  | :----------------------------------------------------------: | :---------: | :-------------------: |
  |    `testWhiteboardComponentChangeCountTestApplicationAdd`    |   **YES**   |                       |
  | `testWhiteboardComponentChangeCountTestApplicationAddResource` |   **NO**    | If catch IAE it works |

+ **JaxRsWhiteboardClientBuilderTests**

  |                    **Test Name**                    | **Passed?** |                      **Failure Reason**                      |
  | :-------------------------------------------------: | :---------: | :----------------------------------------------------------: |
  |             `testClientBuilderService`              |   **NO**    | PrototypeExtension does not set the suffix in the activate method |
  |               `testPromiseRxInvoker`                |   **NO**    |                                                              |
  |                `testSseEventSource`                 |   **NO**    |                    ClassNotFoundException                    |
  | `testWhiteboardComponentApplicationAndResourceTest` |   **NO**    |                    if catch IAE it works                     |

+ **JaxRsWhiteboardComponentTest**

  |                        **Test Name**                         | **Passed?** |  **Failure Reason**   |
  | :----------------------------------------------------------: | :---------: | :-------------------: |
  |          `testWhiteboardComponentLegacyApplication`          |   **YES**   |                       |
  | `testWhiteboardComponentApplicationAndResourceContextPortChange` |   **YES**   |                       |
  | `testWhiteboardComponentDefaultResourceAvailbaleBeforeStart` |   **NO**    | if catch IAE it works |
  |     `testWhiteboardComponentApplicationAndResourceTest`      |   **YES**   |                       |
  |   `testWhiteboardComponentApplicationAndResourceWildcard`    |   **NO**    |                       |
  | `testWhiteboardComponentApplicationAndResourceContextPathChange` |   **YES**   |                       |
  |     `testWhiteboardComponentAnnotatedLegacyApplication`      |   **YES**   |                       |
  |         `testWhiteboardComponentDefaultResourceTest`         |   **NO**    | if catch IAE it works |

+ **JaxRsWhiteboardDefaultAppTests**

  |           **Test Name**           | **Passed?** |                      **Failure Reason**                      |
  | :-------------------------------: | :---------: | :----------------------------------------------------------: |
  |      `testShadowDefaultApp`       |   **YES**   |                                                              |
  | `testComplianceShadowDefaultApp`  |   **NO**    | Specs are in conflict here!! static resources should have lower priority wrt resources registered with the whiteboard (151.4.1.1) |
  |       `testMultipleDefault`       |   **YES**   |                                                              |
  |   `testNameDefaultSubstitution`   |   **YES**   |                                                              |
  | `testBasePathDefaultSubstitution` |   **YES**   |                                                              |

  

+ **JaxRsWhiteboardDTOTests**

  |         **Test Name**         | **Passed?** | **Failure Reason** |
  | :---------------------------: | :---------: | :----------------: |
  |       `testNameFilter`        |   **YES**   |                    |
  | `testNameBindingResourceDTO`  |   **YES**   |                    |
  | `testNameBindingExtensionDTO` |   **YES**   |                    |
  |       `testResourceDTO`       |   **YES**   |                    |
  |      `testExtensionDTO`       |   **YES**   |                    |
  |    `testUngettableService`    |   **YES**   |                    |
  |     `testApplicationDTO`      |   **YES**   |                    |
  |     `testInvalidProperty`     |   **YES**   |                    |

+ **SpecCapabilityTests**

  | **Test Name**                                 | **Passed?** | **Failure Reason** |
  | --------------------------------------------- | ----------- | ------------------ |
  | `testJaxRsWhiteboardImplementationCapability` | **YES**     |                    |
  | `testSseEventSourceFactoryCapability`         | **YES**     |                    |
  | `testJaxRsContractCapability`                 | **YES**     |                    |
  | `testJaxRsServiceRuntimeServiceCapability`    | **YES**     |                    |
  | `testClientBuilderServiceCapability`          | **YES**     |                    |

  