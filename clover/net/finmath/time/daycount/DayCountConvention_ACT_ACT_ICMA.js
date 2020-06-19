var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":103,"id":35768,"methods":[{"el":68,"sc":2,"sl":64},{"el":102,"sc":2,"sl":73}],"name":"DayCountConvention_ACT_ACT_ICMA","sl":51}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_223":{"methods":[{"sl":64}],"name":"testDayCountConventionConsistency_ACT_ACT_ICMA_versus_ACT_ACT_ISDA","pass":true,"statements":[{"sl":65},{"sl":66},{"sl":67}]},"test_563":{"methods":[{"sl":64},{"sl":73}],"name":"testDayCountConventionAdditivity_ACT_ACT_ICMA","pass":true,"statements":[{"sl":65},{"sl":66},{"sl":67},{"sl":75},{"sl":79},{"sl":80},{"sl":82},{"sl":83},{"sl":85},{"sl":86},{"sl":91},{"sl":92},{"sl":94},{"sl":101}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [563, 223], [563, 223], [563, 223], [563, 223], [], [], [], [], [], [563], [], [563], [], [], [], [563], [563], [], [563], [563], [], [563], [563], [], [], [], [], [563], [563], [], [563], [], [], [], [], [], [], [563], [], []]