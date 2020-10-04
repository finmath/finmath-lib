var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":44,"id":23489,"methods":[{"el":33,"sc":2,"sl":30},{"el":43,"sc":2,"sl":40}],"name":"AbstractShortRateVolatilityModel","sl":19}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_238":{"methods":[{"sl":30},{"sl":40}],"name":"testATMSwaptionCalibration","pass":true,"statements":[{"sl":31},{"sl":32},{"sl":42}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [238], [238], [238], [], [], [], [], [], [], [], [238], [], [238], [], []]