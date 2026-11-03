package com.hrpayroll.service;

import com.hrpayroll.model.Decision;
import com.hrpayroll.model.DecisionType;

import java.util.List;

/** Quy tắc đặt mã quyết định: QD-KT-2026-001 (KT khen thưởng, KL kỷ luật). */
public final class DecisionService {

    private DecisionService() {
    }

    /** Phần đầu mã, ví dụ "QD-KT-2026-". */
    public static String codePrefix(DecisionType type, int year) {
        return "QD-" + type.getCodePart() + "-" + year + "-";
    }

    /** Mã kế tiếp theo loại và năm, đánh số tăng dần từ 001. */
    public static String nextCode(
            List<Decision> existing, DecisionType type, int year) {

        String prefix = "QD-" + type.getCodePart() + "-" + year + "-";
        int max = 0;

        for (Decision decision : existing) {
            String code = decision.getCode();

            if (code.startsWith(prefix)) {
                try {
                    max = Math.max(
                        max, Integer.parseInt(code.substring(prefix.length()))
                    );
                } catch (NumberFormatException ex) {
                    // Mã sai dạng không ảnh hưởng việc đánh số.
                }
            }
        }

        return prefix + String.format("%03d", max + 1);
    }
}
