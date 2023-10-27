# Importing a Segment Dataset

Event data can be imported as segments. It's useful when an event log is unavailable or a custom classifier is used. Prepare your segment data in a folder as follows.

## Segment files

1. For each segment `A:B`, create file `log.xes.A!B.seg` (note, the prefix `log.xes` is mandatory).
1. Store each segment occurrence as the following line: `caseID,startTime,ignored,durationMs,class`, for example, `N61579,07-02-2005 00:00:00.000,,432000000,1`. If you do not provide classes for segments, provide `-1` as a class value. Column `ignored` is ignored and can be empty. This is an example of a `.seg` file:  
`caseID,startTime,ignored,durationMs,class`  
`travel permit 4584,01-01-2018 00:00:00.000,,432000000,0`  
`travel permit 4585,02-01-2018 00:00:00.000,,533000000,1`  
`travel permit 4586,03-01-2018 00:00:00.000,,634000000,2`  
1. Create a textual `anyname.segdir` file in your segment directory and describe the segment dataset format. This file must include the following fields (see [example](segment_dataset.segdir)):

| Field |Sample value | Comment |
|:------------- |:-------------|:-----|
| `dateFormat` | `dd-MM-yyyy HH:mm:ss.SSS` | Datetime format in Java [`DateTimeFormatter`](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) format |
| `zoneId` | `Europe/Amsterdam` | Time zone ID in Java [`ZoneId`](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html) format |
| `startTime` | `01-09-2018 00:00:00.000` | Since then the performance spectrum should be computed in the format described above |
| `endTime` | `08-09-2018 00:00:00.000` | Until then the performance spectrum should be computed in the format described above |
| `name` | `My segment classifier`| Name of your classifier (leave blank if you do not provide classes for segments) |
| `legend` | `RandomClass%r0%r1%r2` | Legend for your classifier (leave blank if you do not provide classes for segments)|
| `classCount` | `3` | The class values number (provide 0 if you do not provide classes for segments) |


### ... in ProM

Import your `.segdir` file and use it in the PSM plugin, exactly as XES files.

### ... in standalone PSM 

Import your `.segdir` file and use it exactly as XES files.
