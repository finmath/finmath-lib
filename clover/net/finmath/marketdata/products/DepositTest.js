var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":106,"id":40149,"methods":[{"el":42,"sc":2,"sl":27},{"el":62,"sc":2,"sl":47},{"el":82,"sc":2,"sl":67},{"el":105,"sc":2,"sl":87}],"name":"DepositTest","sl":17}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_109":{"methods":[{"sl":47}],"name":"testRateValueConsistency","pass":true,"statements":[{"sl":51},{"sl":54},{"sl":57},{"sl":60}]},"test_456":{"methods":[{"sl":67}],"name":"testDepositValue","pass":true,"statements":[{"sl":70},{"sl":73},{"sl":76},{"sl":77},{"sl":80}]},"test_553":{"methods":[{"sl":87}],"name":"testEvaluationTime","pass":true,"statements":[{"sl":91},{"sl":92},{"sl":93},{"sl":96},{"sl":99},{"sl":102}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [109], [], [], [], [109], [], [], [109], [], [], [109], [], [], [109], [], [], [], [], [], [], [456], [], [], [456], [], [], [456], [], [], [456], [456], [], [], [456], [], [], [], [], [], [], [553], [], [], [], [553], [553], [553], [], [], [553], [], [], [553], [], [], [553], [], [], [], []]