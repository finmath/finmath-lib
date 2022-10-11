var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":207,"id":15678,"methods":[{"el":56,"sc":2,"sl":52},{"el":69,"sc":2,"sl":66},{"el":168,"sc":2,"sl":71},{"el":172,"sc":2,"sl":170},{"el":176,"sc":2,"sl":174},{"el":187,"sc":2,"sl":178},{"el":206,"sc":2,"sl":189}],"name":"DeltaHedgedPortfolioWithAAD","sl":33}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_127":{"methods":[{"sl":66},{"sl":71},{"sl":170},{"sl":174},{"sl":189}],"name":"testHedgePerformance[MonteCarloAssetModel [model=BlackScholesModel [initialValue=RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@50de0926,\n ID=0], riskFreeRate=RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@2473b9ce,\n ID=1], volatility=RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@60438a68,\n ID=2], randomVariableFactory=RandomVariableDifferentiableAADFactory [diracDeltaApproximationMethod=DISCRETE_DELTA, diracDeltaApproximationWidthPerStdDev=0.0, diracDeltaApproximationDensityRegressionWidthPerStdDev=0.5, isGradientRetainsLeafNodesOnly=false, toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]], initialState=[RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@140e5a13,\n ID=3]], drift=[RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@3439f68d,\n ID=6]], factorLoadings=[RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@60438a68,\n ID=2]]]]-AbstractMonteCarloProduct [currency=null]]","pass":true,"statements":[{"sl":67},{"sl":68},{"sl":75},{"sl":81},{"sl":83},{"sl":84},{"sl":88},{"sl":89},{"sl":90},{"sl":93},{"sl":95},{"sl":98},{"sl":99},{"sl":102},{"sl":103},{"sl":106},{"sl":107},{"sl":108},{"sl":110},{"sl":111},{"sl":113},{"sl":115},{"sl":116},{"sl":119},{"sl":120},{"sl":124},{"sl":126},{"sl":127},{"sl":128},{"sl":132},{"sl":135},{"sl":137},{"sl":144},{"sl":145},{"sl":148},{"sl":149},{"sl":152},{"sl":153},{"sl":161},{"sl":162},{"sl":164},{"sl":167},{"sl":171},{"sl":175},{"sl":190},{"sl":192},{"sl":193},{"sl":196},{"sl":197},{"sl":198},{"sl":199},{"sl":200},{"sl":201},{"sl":205}]},"test_207":{"methods":[{"sl":66},{"sl":71},{"sl":170},{"sl":174},{"sl":189}],"name":"testHedgePerformance[MonteCarloAssetModel [model=BlackScholesModel [initialValue=RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@50de0926,\n ID=0], riskFreeRate=RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@2473b9ce,\n ID=1], volatility=RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@60438a68,\n ID=2], randomVariableFactory=RandomVariableDifferentiableAADFactory [diracDeltaApproximationMethod=DISCRETE_DELTA, diracDeltaApproximationWidthPerStdDev=0.0, diracDeltaApproximationDensityRegressionWidthPerStdDev=0.5, isGradientRetainsLeafNodesOnly=false, toString()=AbstractRandomVariableDifferentiableFactory [randomVariableFactoryForNonDifferentiable=RandomVariableFromArrayFactory [isUseDoublePrecisionFloatingPointImplementation=true]]], initialState=[RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@140e5a13,\n ID=3]], drift=[RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@3439f68d,\n ID=6]], factorLoadings=[RandomVariableDifferentiableAAD [values=net.finmath.stochastic.Scalar@60438a68,\n ID=2]]]]-EuropeanOption [maturity=5.0, strike=1.2840254166877414, underlyingIndex=0, nameOfUnderliyng=null]]","pass":true,"statements":[{"sl":67},{"sl":68},{"sl":75},{"sl":81},{"sl":83},{"sl":84},{"sl":88},{"sl":89},{"sl":93},{"sl":95},{"sl":98},{"sl":99},{"sl":102},{"sl":103},{"sl":106},{"sl":107},{"sl":108},{"sl":110},{"sl":111},{"sl":113},{"sl":115},{"sl":116},{"sl":119},{"sl":120},{"sl":124},{"sl":126},{"sl":127},{"sl":132},{"sl":135},{"sl":137},{"sl":144},{"sl":145},{"sl":148},{"sl":149},{"sl":152},{"sl":153},{"sl":161},{"sl":162},{"sl":164},{"sl":167},{"sl":171},{"sl":175},{"sl":190},{"sl":192},{"sl":193},{"sl":196},{"sl":197},{"sl":198},{"sl":199},{"sl":200},{"sl":201},{"sl":205}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [207, 127], [207, 127], [207, 127], [], [], [207, 127], [], [], [], [207, 127], [], [], [], [], [], [207, 127], [], [207, 127], [207, 127], [], [], [], [207, 127], [207, 127], [127], [], [], [207, 127], [], [207, 127], [], [], [207, 127], [207, 127], [], [], [207, 127], [207, 127], [], [], [207, 127], [207, 127], [207, 127], [], [207, 127], [207, 127], [], [207, 127], [], [207, 127], [207, 127], [], [], [207, 127], [207, 127], [], [], [], [207, 127], [], [207, 127], [207, 127], [127], [], [], [], [207, 127], [], [], [207, 127], [], [207, 127], [], [], [], [], [], [], [207, 127], [207, 127], [], [], [207, 127], [207, 127], [], [], [207, 127], [207, 127], [], [], [], [], [], [], [], [207, 127], [207, 127], [], [207, 127], [], [], [207, 127], [], [], [207, 127], [207, 127], [], [], [207, 127], [207, 127], [], [], [], [], [], [], [], [], [], [], [], [], [], [207, 127], [207, 127], [], [207, 127], [207, 127], [], [], [207, 127], [207, 127], [207, 127], [207, 127], [207, 127], [207, 127], [], [], [], [207, 127], [], []]