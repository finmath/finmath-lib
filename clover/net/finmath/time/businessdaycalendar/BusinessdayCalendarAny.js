var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":36,"id":35453,"methods":[{"el":25,"sc":2,"sl":24},{"el":30,"sc":2,"sl":27},{"el":35,"sc":2,"sl":32}],"name":"BusinessdayCalendarAny","sl":17}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_459":{"methods":[{"sl":24}],"name":"testCDS","pass":true,"statements":[]},"test_632":{"methods":[{"sl":24},{"sl":27}],"name":"testCreateDateFromDateAndOffsetCode","pass":true,"statements":[{"sl":29}]},"test_803":{"methods":[{"sl":24}],"name":"testCDS","pass":true,"statements":[]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [459, 803, 632], [], [], [632], [], [632], [], [], [], [], [], [], []]