package com.realdev.readle.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Claude AI 등의 LLM 응답 텍스트에서 유효한 JSON (객체 또는 배열) 부분만 안전하게 추출하는 유틸리티 클래스. */
public class JsonExtractor {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonExtractor() {
    // 유틸리티 클래스 인스턴스화 방지
  }

  /**
   * 입력 텍스트에서 마크다운 블록(```json ... ```) 또는 중첩 깊이(depth)가 균형을 이루며 유효하게 파싱되는 첫번째 JSON 객체({...}) 혹은
   * 배열([...]) 영역을 추출합니다.
   *
   * @param text LLM 원본 응답 텍스트
   * @return 추출된 정제된 JSON 문자열 (입력이 null이거나 빈 경우 빈 문자열 반환)
   */
  public static String extractJson(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }

    String trimmed = text.trim();

    // 1. 마크다운 코드 블록(```json ... ``` 또는 ``` ... ```) 제거 시도
    if (trimmed.startsWith("```")) {
      int firstNewLine = trimmed.indexOf('\n');
      int lastBackticks = trimmed.lastIndexOf("```");

      if (firstNewLine != -1 && lastBackticks > firstNewLine) {
        trimmed = trimmed.substring(firstNewLine + 1, lastBackticks).trim();
      } else {
        // 단일 줄 ```json {"a":1} ``` 형태 등 예외적 마크다운 대처
        trimmed = trimmed.replaceAll("^```(?:json)?", "").replaceAll("```$", "").trim();
      }
    }

    // 2. 균형을 이루는 유효한 JSON 후보 (Balanced Valid Candidate) 스캔
    String balancedCandidate = findBalancedCandidate(trimmed);
    if (!balancedCandidate.isEmpty()) {
      return balancedCandidate;
    }

    return trimmed;
  }

  private static String findBalancedCandidate(String s) {
    // 루트가 '{' 로 시작하지만 닫히지 않은 유실된 미완성 JSON 객체인 경우
    // 내부 하위 배열/객체 우발 파싱을 막기 위해 렌더링 중단
    if (s.startsWith("{") && isUnclosedRootBrace(s)) {
      return "";
    }

    int length = s.length();

    for (int start = 0; start < length; start++) {
      char openChar = s.charAt(start);
      if (openChar != '{' && openChar != '[') {
        continue;
      }
      char closeChar = (openChar == '{') ? '}' : ']';

      int depth = 0;
      boolean inString = false;
      boolean escaped = false;

      for (int i = start; i < length; i++) {
        char c = s.charAt(i);

        if (escaped) {
          escaped = false;
          continue;
        }

        if (c == '\\' && inString) {
          escaped = true;
          continue;
        }

        if (c == '"') {
          inString = !inString;
          continue;
        }

        if (!inString) {
          if (c == openChar) {
            depth++;
          } else if (c == closeChar) {
            depth--;
            if (depth == 0) {
              String candidate = s.substring(start, i + 1).trim();
              if (isValidJson(candidate)) {
                return candidate;
              }
              break;
            }
          }
        }
      }
    }

    return "";
  }

  private static boolean isUnclosedRootBrace(String s) {
    int depth = 0;
    boolean inString = false;
    boolean escaped = false;

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (c == '\\' && inString) {
        escaped = true;
        continue;
      }
      if (c == '"') {
        inString = !inString;
        continue;
      }
      if (!inString) {
        if (c == '{') {
          depth++;
        } else if (c == '}') {
          depth--;
          if (depth == 0) {
            return false;
          }
        }
      }
    }
    return depth > 0;
  }

  private static boolean isValidJson(String str) {
    try {
      OBJECT_MAPPER.readTree(str);
      return true;
    } catch (JsonProcessingException e) {
      return false;
    }
  }
}
