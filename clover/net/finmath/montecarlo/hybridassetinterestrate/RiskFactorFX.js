var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":16,"id":20051,"methods":[{"el":10,"sc":2,"sl":7},{"el":15,"sc":2,"sl":12}],"name":"RiskFactorFX","sl":3}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_1001":{"methods":[{"sl":7},{"sl":12}],"name":"testForeignCaplet","pass":true,"statements":[{"sl":8},{"sl":9},{"sl":14}]},"test_1034":{"methods":[{"sl":7},{"sl":12}],"name":"testProperties","pass":true,"statements":[{"sl":8},{"sl":9},{"sl":14}]},"test_11":{"methods":[{"sl":7},{"sl":12}],"name":"testForeignFRA","pass":true,"statements":[{"sl":8},{"sl":9},{"sl":14}]},"test_1143":{"methods":[{"sl":7},{"sl":12}],"name":"testForeignBond","pass":true,"statements":[{"sl":8},{"sl":9},{"sl":14}]},"test_1190":{"methods":[{"sl":7},{"sl":12}],"name":"testForeignFRA","pass":true,"statements":[{"sl":8},{"sl":9},{"sl":14}]},"test_181":{"methods":[{"sl":7},{"sl":12}],"name":"testForeignBond","pass":true,"statements":[{"sl":8},{"sl":9},{"sl":14}]},"test_869":{"methods":[{"sl":7},{"sl":12}],"name":"testForeignCaplet","pass":true,"statements":[{"sl":8},{"sl":9},{"sl":14}]},"test_999":{"methods":[{"sl":7},{"sl":12}],"name":"testProperties","pass":true,"statements":[{"sl":8},{"sl":9},{"sl":14}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [869, 999, 1001, 181, 1143, 11, 1034, 1190], [869, 999, 1001, 181, 1143, 11, 1034, 1190], [869, 999, 1001, 181, 1143, 11, 1034, 1190], [], [], [869, 999, 1001, 181, 1143, 11, 1034, 1190], [], [869, 999, 1001, 181, 1143, 11, 1034, 1190], [], []]