# Transforming a Dataset of Segments for Performance Spectrum Analysis

Data for converting into a Performance Spectrum can be repreented as segments. That is useful when building a Performance Spectrum form an event log cannot produce a desirable view on data. Prepare your segment data as follows.

## Segment files

1. For each segment `A:B` create file `log.xes.A!B.seg`
1. Store each occurence of the segment in this file as following line: `CaseID,unix_timestamp_UTC_ms,segment_name,duration_ms,class`, for example, `N61579,07-02-2005 00:00:00.000,Add penalty:Appeal to Judge,432000000,1`. If you do not provide classes for segments, provide `-1` as a class value.
1. Create a textual (ini) `.segdir` file in your segment directory and provide a description of the segment dataset. This file must include the following fields (see [example](segment_dataset.segdir)):

| Field |Sample value | Comment |
|:------------- |:-------------|:-----|
| `dateFormat` | `dd-MM-yyyy HH:mm:ss.SSS` | Datetime format in Java [`DateTimeFormatter`](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) format |
| `zoneId` | `Europe/Amsterdam` | Time zone ID in Java [`ZoneId`](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html) format |
| `startTime` | `01-09-2018 00:00:00.000` | Since then the performance spectrum should be computed, in the format described above |
| `endTime` | `08-09-2018 00:00:00.000` | Until then the performance spectrum should be computed, in the format described above |
| `name` | `My segment classifier`| Name of your classifier (leave blank if you do not provide classes for segments) |
| `legend` | `RandomClass%r0%r1%r2` | Legend for your classifier (leave blank if you do not provide classes for segments)|
| `classCount` | `3` | The class values number (provide 0 if you do not provide classes for segments) |


### ... in ProM

Import your `.segdir` file and use it in the PSM plugin, exactly as XES files.

### ... in PSM standalone

Import your `.segdir` file and use it exactly as XES files.
