var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":75,"id":27356,"methods":[{"el":41,"sc":2,"sl":35},{"el":55,"sc":2,"sl":49},{"el":60,"sc":2,"sl":57},{"el":74,"sc":2,"sl":62}],"name":"LaggedIndex","sl":24}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_135":{"methods":[{"sl":49},{"sl":62}],"name":"testLIBORInArrearsFloatLeg","pass":true,"statements":[{"sl":50},{"sl":51},{"sl":52},{"sl":53},{"sl":54},{"sl":64},{"sl":72}]},"test_272":{"methods":[{"sl":49},{"sl":62}],"name":"testLIBORInArrearsFloatLeg","pass":true,"statements":[{"sl":50},{"sl":51},{"sl":52},{"sl":53},{"sl":54},{"sl":64},{"sl":72}]},"test_91":{"methods":[{"sl":49}],"name":"testLIBORInArrearsFloatLeg","pass":true,"statements":[{"sl":50},{"sl":51},{"sl":52},{"sl":53},{"sl":54}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [91, 272, 135], [91, 272, 135], [91, 272, 135], [91, 272, 135], [91, 272, 135], [91, 272, 135], [], [], [], [], [], [], [], [272, 135], [], [272, 135], [], [], [], [], [], [], [], [272, 135], [], [], []]