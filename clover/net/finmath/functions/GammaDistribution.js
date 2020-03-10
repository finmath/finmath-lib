var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":31,"id":1541,"methods":[{"el":20,"sc":2,"sl":17},{"el":30,"sc":2,"sl":28}],"name":"GammaDistribution","sl":13}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_355":{"methods":[{"sl":17},{"sl":28}],"name":"testMartingalePropertyMonteCarlo","pass":true,"statements":[{"sl":18},{"sl":19},{"sl":29}]},"test_410":{"methods":[{"sl":17},{"sl":28}],"name":"test","pass":true,"statements":[{"sl":18},{"sl":19},{"sl":29}]},"test_690":{"methods":[{"sl":17},{"sl":28}],"name":"testCharacteristicFunction","pass":true,"statements":[{"sl":18},{"sl":19},{"sl":29}]},"test_885":{"methods":[{"sl":17},{"sl":28}],"name":"testScaling","pass":true,"statements":[{"sl":18},{"sl":19},{"sl":29}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [410, 690, 885, 355], [410, 690, 885, 355], [410, 690, 885, 355], [], [], [], [], [], [], [], [], [410, 690, 885, 355], [410, 690, 885, 355], [], []]