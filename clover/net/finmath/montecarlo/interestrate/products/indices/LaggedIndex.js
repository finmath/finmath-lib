var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":75,"id":29202,"methods":[{"el":41,"sc":2,"sl":35},{"el":55,"sc":2,"sl":49},{"el":60,"sc":2,"sl":57},{"el":74,"sc":2,"sl":62}],"name":"LaggedIndex","sl":24}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_210":{"methods":[{"sl":49}],"name":"testLIBORInArrearsFloatLeg","pass":true,"statements":[{"sl":50},{"sl":51},{"sl":52},{"sl":53},{"sl":54}]},"test_529":{"methods":[{"sl":49},{"sl":62}],"name":"testLIBORInArrearsFloatLeg","pass":true,"statements":[{"sl":50},{"sl":51},{"sl":52},{"sl":53},{"sl":54},{"sl":64},{"sl":72}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [210, 529], [210, 529], [210, 529], [210, 529], [210, 529], [210, 529], [], [], [], [], [], [], [], [529], [], [529], [], [], [], [], [], [], [], [529], [], [], []]