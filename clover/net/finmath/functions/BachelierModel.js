var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":823,"id":1665,"methods":[{"el":46,"sc":2,"sl":44},{"el":90,"sc":2,"sl":67},{"el":130,"sc":2,"sl":110},{"el":179,"sc":2,"sl":150},{"el":221,"sc":2,"sl":199},{"el":267,"sc":2,"sl":245},{"el":306,"sc":2,"sl":287},{"el":349,"sc":2,"sl":326},{"el":388,"sc":2,"sl":369},{"el":417,"sc":2,"sl":409},{"el":446,"sc":2,"sl":438},{"el":475,"sc":2,"sl":467},{"el":505,"sc":2,"sl":497},{"el":535,"sc":2,"sl":527},{"el":564,"sc":2,"sl":556},{"el":593,"sc":2,"sl":585},{"el":624,"sc":2,"sl":613},{"el":655,"sc":2,"sl":644},{"el":689,"sc":2,"sl":676},{"el":720,"sc":2,"sl":709},{"el":756,"sc":2,"sl":745},{"el":789,"sc":2,"sl":776},{"el":822,"sc":2,"sl":809}],"name":"BachelierModel","sl":41}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_10":{"methods":[{"sl":67},{"sl":150}],"name":"testCalibration","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_105":{"methods":[{"sl":67}],"name":"testExpectation","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88}]},"test_110":{"methods":[{"sl":67}],"name":"testSwaption","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78}]},"test_131":{"methods":[{"sl":67},{"sl":150}],"name":"testATMSwaptionCalibration","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_169":{"methods":[{"sl":67},{"sl":199}],"name":"testBachelierOptionDelta","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":206},{"sl":209},{"sl":210},{"sl":215},{"sl":217},{"sl":219}]},"test_174":{"methods":[{"sl":67},{"sl":199},{"sl":409},{"sl":497}],"name":"testEuropeanCallDelta[RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=false]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":416},{"sl":504}]},"test_215":{"methods":[{"sl":67},{"sl":199},{"sl":409},{"sl":497},{"sl":613},{"sl":709}],"name":"testInhomogenousDelta","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":416},{"sl":504},{"sl":620},{"sl":621},{"sl":623},{"sl":716},{"sl":717},{"sl":719}]},"test_217":{"methods":[{"sl":67},{"sl":326},{"sl":409},{"sl":556}],"name":"testEuropeanCallVega[RandomVariableDifferentiableAADFactory [diracDeltaApproximationMethod=DISCRETE_DELTA, diracDeltaApproximationWidthPerStdDev=0.05, diracDeltaApproximationDensityRegressionWidthPerStdDev=0.5, isGradientRetainsLeafNodesOnly=true, toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":416},{"sl":563}]},"test_219":{"methods":[{"sl":67},{"sl":150}],"name":"testSimplified","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_234":{"methods":[{"sl":67},{"sl":110},{"sl":199},{"sl":287},{"sl":326},{"sl":369},{"sl":409},{"sl":438},{"sl":497},{"sl":527},{"sl":556},{"sl":585},{"sl":613},{"sl":644},{"sl":709},{"sl":745},{"sl":776},{"sl":809}],"name":"testInhomogeneousRandomVariableImplementations","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":117},{"sl":122},{"sl":123},{"sl":125},{"sl":128},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":294},{"sl":299},{"sl":300},{"sl":302},{"sl":304},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":376},{"sl":381},{"sl":382},{"sl":384},{"sl":386},{"sl":416},{"sl":445},{"sl":504},{"sl":534},{"sl":563},{"sl":592},{"sl":620},{"sl":621},{"sl":623},{"sl":652},{"sl":654},{"sl":716},{"sl":717},{"sl":719},{"sl":753},{"sl":755},{"sl":783},{"sl":784},{"sl":786},{"sl":788},{"sl":817},{"sl":819},{"sl":821}]},"test_241":{"methods":[{"sl":67},{"sl":150}],"name":"testCapletSmile","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_248":{"methods":[{"sl":67},{"sl":326},{"sl":409},{"sl":556}],"name":"testEuropeanCallVega[RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=false]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":416},{"sl":563}]},"test_265":{"methods":[{"sl":67},{"sl":326},{"sl":409},{"sl":556}],"name":"testEuropeanCallVega[RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":416},{"sl":563}]},"test_279":{"methods":[{"sl":67},{"sl":409}],"name":"testEuropeanCall[RandomVariableDifferentiableAADFactory [diracDeltaApproximationMethod=DISCRETE_DELTA, diracDeltaApproximationWidthPerStdDev=0.05, diracDeltaApproximationDensityRegressionWidthPerStdDev=0.5, isGradientRetainsLeafNodesOnly=true, toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":416}]},"test_28":{"methods":[{"sl":67},{"sl":150}],"name":"testBasic","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_281":{"methods":[{"sl":67},{"sl":199},{"sl":409},{"sl":497}],"name":"testEuropeanCallDelta[RandomVariableDifferentiableADFactory [toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":416},{"sl":504}]},"test_305":{"methods":[{"sl":67},{"sl":150},{"sl":409},{"sl":467}],"name":"testImpliedVolatility","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178},{"sl":416},{"sl":474}]},"test_309":{"methods":[{"sl":67},{"sl":409}],"name":"testEuropeanCall[RandomVariableDifferentiableADFactory [toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":416}]},"test_311":{"methods":[{"sl":67},{"sl":409}],"name":"testEuropeanCall[RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":416}]},"test_315":{"methods":[{"sl":67},{"sl":150}],"name":"testBachelierOptionImpliedVolatility","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_329":{"methods":[{"sl":67},{"sl":150}],"name":"testCapletSmile[RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=false]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_337":{"methods":[{"sl":67},{"sl":199},{"sl":409},{"sl":497}],"name":"testEuropeanCallDelta[RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":416},{"sl":504}]},"test_342":{"methods":[{"sl":67},{"sl":150}],"name":"testMultiPiterbarg","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_354":{"methods":[{"sl":67}],"name":"testSwaption","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88}]},"test_361":{"methods":[{"sl":67},{"sl":150}],"name":"testVolatilityConversionLognormalToNormal","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_364":{"methods":[{"sl":110}],"name":"testATMSwaptionCalibration[VOLATILITYNORMAL-STOCHASTIC_LEVENBERG_MARQUARDT-ADJOINT_AUTOMATIC_DIFFERENTIATION]","pass":true,"statements":[{"sl":117},{"sl":122},{"sl":123},{"sl":125},{"sl":128}]},"test_39":{"methods":[{"sl":67},{"sl":110},{"sl":199},{"sl":287},{"sl":326},{"sl":369},{"sl":409},{"sl":438},{"sl":497},{"sl":527},{"sl":556},{"sl":585}],"name":"testHomogeneousRandomVariableImplementations","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":117},{"sl":122},{"sl":123},{"sl":125},{"sl":128},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":294},{"sl":299},{"sl":300},{"sl":302},{"sl":304},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":376},{"sl":381},{"sl":382},{"sl":384},{"sl":386},{"sl":416},{"sl":445},{"sl":504},{"sl":534},{"sl":563},{"sl":592}]},"test_397":{"methods":[{"sl":199}],"name":"testGenericDelta[Caplet maturity 10.0]","pass":true,"statements":[{"sl":206},{"sl":209},{"sl":210}]},"test_40":{"methods":[{"sl":67},{"sl":409}],"name":"testEuropeanCall[RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=false]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":416}]},"test_420":{"methods":[{"sl":67},{"sl":150}],"name":"testATMSwaptionCalibration[Model: NORMAL, Calibration: MONTECARLO]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_423":{"methods":[{"sl":67},{"sl":326},{"sl":409},{"sl":556},{"sl":613},{"sl":776}],"name":"testInhomogenousVega","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":416},{"sl":563},{"sl":620},{"sl":621},{"sl":623},{"sl":783},{"sl":784},{"sl":786},{"sl":788}]},"test_427":{"methods":[{"sl":67},{"sl":409},{"sl":613}],"name":"testEuropeanCall","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":416},{"sl":620},{"sl":621},{"sl":623}]},"test_436":{"methods":[{"sl":199}],"name":"testGenericDelta[Caplet maturity 5.0]","pass":true,"statements":[{"sl":206},{"sl":209},{"sl":210}]},"test_439":{"methods":[{"sl":67}],"name":"testStaticCubeCalibration","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88}]},"test_443":{"methods":[{"sl":67},{"sl":150}],"name":"testSimplifiedLinear","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_498":{"methods":[{"sl":67},{"sl":150}],"name":"testVolatilityCalibration[VOLATILITYNORMAL]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_525":{"methods":[{"sl":67}],"name":"testBermudanSwaption","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78}]},"test_527":{"methods":[{"sl":67},{"sl":150}],"name":"testATMSwaptionCalibration[Model: NORMAL, Calibration: ANALYTIC]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_528":{"methods":[{"sl":67},{"sl":150}],"name":"c_testMultiPiterbarg","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_53":{"methods":[{"sl":67},{"sl":199},{"sl":409},{"sl":497}],"name":"testDelta","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":416},{"sl":504}]},"test_532":{"methods":[{"sl":67},{"sl":326},{"sl":409},{"sl":556}],"name":"testVega","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":416},{"sl":563}]},"test_535":{"methods":[{"sl":67},{"sl":150}],"name":"b_testBasicPiterbarg","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_54":{"methods":[{"sl":67}],"name":"testBachelierRiskNeutralProbabilities","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88}]},"test_550":{"methods":[{"sl":67},{"sl":199},{"sl":409},{"sl":497},{"sl":613},{"sl":709}],"name":"testEuropeanCallDelta","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":416},{"sl":504},{"sl":620},{"sl":621},{"sl":623},{"sl":716},{"sl":717},{"sl":719}]},"test_551":{"methods":[{"sl":67},{"sl":150}],"name":"testCapletSmile[RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_557":{"methods":[{"sl":67},{"sl":326},{"sl":409},{"sl":556},{"sl":613},{"sl":776}],"name":"testEuropeanCallVega","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":416},{"sl":563},{"sl":620},{"sl":621},{"sl":623},{"sl":783},{"sl":784},{"sl":786},{"sl":788}]},"test_579":{"methods":[{"sl":67},{"sl":150}],"name":"a_testSimplifiedLinear","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_584":{"methods":[{"sl":67},{"sl":150}],"name":"testSingleSmile","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_60":{"methods":[{"sl":67},{"sl":199},{"sl":409},{"sl":497}],"name":"testEuropeanCallDelta[RandomVariableDifferentiableAADFactory [diracDeltaApproximationMethod=DISCRETE_DELTA, diracDeltaApproximationWidthPerStdDev=0.05, diracDeltaApproximationDensityRegressionWidthPerStdDev=0.5, isGradientRetainsLeafNodesOnly=true, toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":206},{"sl":209},{"sl":215},{"sl":217},{"sl":219},{"sl":416},{"sl":504}]},"test_600":{"methods":[{"sl":67},{"sl":150}],"name":"testCalibration","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_604":{"methods":[{"sl":67},{"sl":150}],"name":"testCapletSmile[RandomVariableDifferentiableADFactory [toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_619":{"methods":[{"sl":67},{"sl":150}],"name":"testCaplet","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_622":{"methods":[{"sl":110}],"name":"testATMSwaptionCalibration[VOLATILITYNORMAL-STOCHASTIC_LEVENBERG_MARQUARDT-FINITE_DIFFERENCES]","pass":true,"statements":[{"sl":117},{"sl":122},{"sl":123},{"sl":125},{"sl":128}]},"test_625":{"methods":[{"sl":67},{"sl":150}],"name":"testBasicPiterbarg","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_628":{"methods":[{"sl":67},{"sl":326},{"sl":409},{"sl":556}],"name":"testEuropeanCallVega[RandomVariableDifferentiableADFactory [toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":333},{"sl":336},{"sl":343},{"sl":345},{"sl":347},{"sl":416},{"sl":563}]},"test_64":{"methods":[{"sl":67},{"sl":150}],"name":"testMulti","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_643":{"methods":[{"sl":67},{"sl":150}],"name":"testSABRCubeCalibration","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_646":{"methods":[{"sl":67},{"sl":150}],"name":"testCapletSmile[RandomVariableDifferentiableAADFactory [diracDeltaApproximationMethod=DISCRETE_DELTA, diracDeltaApproximationWidthPerStdDev=0.05, diracDeltaApproximationDensityRegressionWidthPerStdDev=0.5, isGradientRetainsLeafNodesOnly=true, toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]]]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_85":{"methods":[{"sl":67},{"sl":150},{"sl":409},{"sl":467},{"sl":613},{"sl":676}],"name":"testInhomogeneousImpliedVolatility","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178},{"sl":416},{"sl":474},{"sl":620},{"sl":621},{"sl":623},{"sl":683},{"sl":685},{"sl":686},{"sl":688}]},"test_86":{"methods":[{"sl":67},{"sl":150}],"name":"testATMSwaptionCalibration[Model: DISPLACED, Calibration: MONTECARLO]","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]},"test_96":{"methods":[{"sl":67},{"sl":150}],"name":"testConversions","pass":true,"statements":[{"sl":74},{"sl":77},{"sl":78},{"sl":83},{"sl":85},{"sl":88},{"sl":159},{"sl":160},{"sl":163},{"sl":164},{"sl":167},{"sl":168},{"sl":169},{"sl":171},{"sl":173},{"sl":175},{"sl":178}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [551, 86, 174, 311, 39, 215, 28, 329, 342, 550, 600, 265, 281, 361, 579, 219, 337, 234, 427, 535, 532, 40, 309, 54, 241, 443, 528, 217, 64, 315, 53, 439, 619, 498, 527, 423, 10, 525, 110, 643, 131, 584, 96, 248, 354, 420, 279, 305, 105, 557, 646, 625, 604, 60, 85, 169, 628], [], [], [], [], [], [], [551, 86, 174, 311, 39, 215, 28, 329, 342, 550, 600, 265, 281, 361, 579, 219, 337, 234, 427, 535, 532, 40, 309, 54, 241, 443, 528, 217, 64, 315, 53, 439, 619, 498, 527, 423, 10, 525, 110, 643, 131, 584, 96, 248, 354, 420, 279, 305, 105, 557, 646, 625, 604, 60, 85, 169, 628], [], [], [551, 86, 174, 311, 39, 215, 28, 329, 342, 550, 600, 265, 281, 361, 579, 219, 337, 234, 427, 535, 532, 40, 309, 54, 241, 443, 528, 217, 64, 315, 53, 439, 619, 498, 527, 423, 10, 525, 110, 643, 131, 584, 96, 248, 354, 420, 279, 305, 105, 557, 646, 625, 604, 60, 85, 169, 628], [86, 311, 28, 342, 600, 361, 579, 219, 427, 535, 40, 309, 443, 528, 64, 315, 439, 527, 10, 525, 110, 643, 131, 584, 96, 354, 420, 279, 305, 105, 625, 85], [], [], [], [], [551, 174, 311, 39, 215, 28, 329, 342, 550, 600, 265, 281, 579, 219, 337, 234, 427, 535, 532, 40, 309, 54, 241, 443, 528, 217, 64, 315, 53, 439, 619, 498, 423, 10, 643, 584, 96, 248, 354, 279, 305, 105, 557, 646, 625, 604, 60, 85, 169, 628], [], [551, 174, 311, 39, 215, 28, 329, 342, 550, 600, 265, 281, 579, 219, 337, 234, 427, 535, 532, 40, 309, 54, 241, 443, 528, 217, 64, 315, 53, 439, 619, 498, 423, 10, 643, 584, 96, 248, 354, 279, 305, 105, 557, 646, 625, 604, 60, 85, 169, 628], [], [], [551, 174, 311, 39, 215, 28, 329, 342, 550, 600, 265, 281, 579, 219, 337, 234, 427, 535, 532, 40, 309, 54, 241, 443, 528, 217, 64, 315, 53, 439, 619, 498, 423, 10, 643, 584, 96, 248, 354, 279, 305, 105, 557, 646, 625, 604, 60, 85, 169, 628], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [39, 622, 234, 364], [], [], [], [], [], [], [39, 622, 234, 364], [], [], [], [], [39, 622, 234, 364], [39, 622, 234, 364], [], [39, 622, 234, 364], [], [], [39, 622, 234, 364], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [], [], [], [], [], [], [], [], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [], [], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [], [], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [], [], [551, 86, 28, 329, 342, 600, 361, 579, 219, 535, 241, 443, 528, 64, 315, 619, 498, 527, 10, 643, 131, 584, 96, 420, 305, 646, 625, 604, 85], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [174, 39, 215, 550, 281, 337, 397, 436, 234, 53, 60, 169], [], [], [], [], [], [], [174, 39, 215, 550, 281, 337, 397, 436, 234, 53, 60, 169], [], [], [174, 39, 215, 550, 281, 337, 397, 436, 234, 53, 60, 169], [397, 436, 169], [], [], [], [], [174, 39, 215, 550, 281, 337, 234, 53, 60, 169], [], [174, 39, 215, 550, 281, 337, 234, 53, 60, 169], [], [174, 39, 215, 550, 281, 337, 234, 53, 60, 169], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [39, 234], [], [], [], [], [], [], [39, 234], [], [], [], [], [39, 234], [39, 234], [], [39, 234], [], [39, 234], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [39, 265, 234, 532, 217, 423, 248, 557, 628], [], [], [], [], [], [], [39, 265, 234, 532, 217, 423, 248, 557, 628], [], [], [39, 265, 234, 532, 217, 423, 248, 557, 628], [], [], [], [], [], [], [39, 265, 234, 532, 217, 423, 248, 557, 628], [], [39, 265, 234, 532, 217, 423, 248, 557, 628], [], [39, 265, 234, 532, 217, 423, 248, 557, 628], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [39, 234], [], [], [], [], [], [], [39, 234], [], [], [], [], [39, 234], [39, 234], [], [39, 234], [], [39, 234], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [174, 311, 39, 215, 550, 265, 281, 337, 234, 427, 532, 40, 309, 217, 53, 423, 248, 279, 305, 557, 60, 85, 628], [], [], [], [], [], [], [174, 311, 39, 215, 550, 265, 281, 337, 234, 427, 532, 40, 309, 217, 53, 423, 248, 279, 305, 557, 60, 85, 628], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [39, 234], [], [], [], [], [], [], [39, 234], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [305, 85], [], [], [], [], [], [], [305, 85], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [174, 39, 215, 550, 281, 337, 234, 53, 60], [], [], [], [], [], [], [174, 39, 215, 550, 281, 337, 234, 53, 60], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [39, 234], [], [], [], [], [], [], [39, 234], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [39, 265, 234, 532, 217, 423, 248, 557, 628], [], [], [], [], [], [], [39, 265, 234, 532, 217, 423, 248, 557, 628], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [39, 234], [], [], [], [], [], [], [39, 234], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [215, 550, 234, 427, 423, 557, 85], [], [], [], [], [], [], [215, 550, 234, 427, 423, 557, 85], [215, 550, 234, 427, 423, 557, 85], [], [215, 550, 234, 427, 423, 557, 85], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [234], [], [], [], [], [], [], [], [234], [], [234], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [85], [], [], [], [], [], [], [85], [], [85], [85], [], [85], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [215, 550, 234], [], [], [], [], [], [], [215, 550, 234], [215, 550, 234], [], [215, 550, 234], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [234], [], [], [], [], [], [], [], [234], [], [234], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [234, 423, 557], [], [], [], [], [], [], [234, 423, 557], [234, 423, 557], [], [234, 423, 557], [], [234, 423, 557], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [234], [], [], [], [], [], [], [], [234], [], [234], [], [234], [], []]