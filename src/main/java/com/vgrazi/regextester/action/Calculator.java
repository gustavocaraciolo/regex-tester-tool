package com.vgrazi.regextester.action;

import com.vgrazi.regextester.component.ColorRange;
import com.vgrazi.regextester.component.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.vgrazi.regextester.component.Constants.GROUP_COLOR;
import static com.vgrazi.regextester.component.Constants.HIGHLIGHT_COLOR;

/**
 * Calculates all of the groups in a Regex pattern, calculates all of the matches in a string, etc
 */
public class Calculator {

    /**
     * Returns a list of ColorRanges of all capture groups in the supplied regex. These are in order,
     * beginning with capture group 1 in position 0 of the array
     * @param regex
     * @param color
     * @return
     * @throws UnmatchedLeftParenException
     */
    public static ColorRange[] parseGroupRanges(String regex, Color color) throws UnmatchedLeftParenException
    {
        List<ColorRange> list = new ArrayList<>();
        Stack<ColorRange> stack = new Stack<>();
        for(int index = 0; index < regex.length(); index++) {
            char ch = regex.charAt(index);
            if(ch == '(') {
                if(isNotEscaped(regex, index)) {
                    // for now, set the size == 0. We will adjust it as required when a right paren turns up
                    ColorRange colorRange = new ColorRange(color, index, 0, false);
                    stack.push(colorRange);
                    list.add(colorRange);
                }
            }
            else if(ch == ')') {
                if (isNotEscaped(regex, index)) {
                    if(!stack.isEmpty()) {
                        ColorRange colorRange = stack.pop();
                        colorRange.setEnd(index);
                    }
                    else {
                        throw new UnmatchedLeftParenException(index);
                    }
                }
            }
        }
        // find all left and right parens and return as 1 character ranges
        ColorRange[] ranges = new ColorRange[list.size()];
        list.toArray(ranges);
        return ranges;
    }

    /**
     * Give that the user selected the Find radio, calculates the highlight ranges for all matches
     * @param matcher
     * @return
     */
    static List<ColorRange> processFindCommand(Matcher matcher) {
        List<ColorRange> list = new ArrayList<>();
        while(matcher.find()) {
            processCommand(matcher, list);
        }
        return list;
    }

    /**
     * Give that the user selected the Looking at radio, calculates the highlight range, if any
     * @param matcher
     * @return
     */
    static List<ColorRange> processLookingAtCommand(Matcher matcher) {
        List<ColorRange> list = new ArrayList<>();
        if(matcher.lookingAt()) {
            processCommand(matcher, list);
        }
        return list;
    }

    /**
     * Give that the user selected the Matches radio, calculates the highlight ranges for the match, if any
     * @param matcher
     * @return
     */
    static List<ColorRange> processMatchesCommand(Matcher matcher) {
        List<ColorRange> list = new ArrayList<>();
        if(matcher.matches()) {
            processCommand(matcher, list);
        }
        return list;
    }

    static List<ColorRange> processSplitCommand(JTextPane auxiliaryPane, String text, Pattern pattern, Matcher matcher) {
        List<ColorRange> list;
        list = processFindCommand(matcher);
        String[] split = pattern.split(text);
        String splitString = String.join("\n", split);
        auxiliaryPane.setText(splitString);
        return list;
    }

    static List<ColorRange> processReplaceAllCommand(JTextPane auxiliaryPanel, JTextPane replacementPane, Matcher matcher) {
        List<ColorRange> list;
        list = processFindCommand(matcher);
        SwingUtilities.invokeLater(() -> {
            try {
                String replacement = replacementPane.getText();
                final String replaced = matcher.replaceAll(replacement);
                auxiliaryPanel.setText(replaced);
            } catch (Exception e) {
                auxiliaryPanel.setText("");
                replacementPane.setBorder(Constants.RED_BORDER);
            }
        });
        return list;
    }

    /**
     * Used by all of the find, match, etc radios, calculates the next range in the matcher, and adds it to the supplied list
     * @param matcher
     * @param list
     */
    private static void processCommand(Matcher matcher, List<ColorRange> list) {
        int start = matcher.start();
        int end = matcher.end();
        ColorRange range = new ColorRange(HIGHLIGHT_COLOR, start, end-1, true);
        list.add(range);
    }

    /**
     * Returns a list of all ranges from the character pane that match the group indexed.
     * @param characterPane
     * @param groupIndex
     * @param regex
     * @param flags
     * @return
     * @throws PatternSyntaxException
     */
    public static List<ColorRange> calculateMatchingGroups(JTextPane characterPane, int groupIndex, String regex, int flags) throws PatternSyntaxException {
        List<ColorRange> list = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(characterPane.getText());
        while(matcher.find()) {
            int start = matcher.start(groupIndex);
            int end = matcher.end(groupIndex) - 1;
            ColorRange range = new ColorRange(GROUP_COLOR, start, end, true);
            list.add(range);
        }
        return list;
    }

    /**
     * Returns a list of all ranges from the character pane that match the group indexed.
     * @param characterPane
     * @param groupName
     * @param regex
     * @param flags
     * @return
     * @throws PatternSyntaxException
     */
    public static List<ColorRange> calculateMatchingGroup(JTextPane characterPane, String groupName, String regex, int flags) {

        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(characterPane.getText());

        // do the finds first, then the groups, so that the group highlighting will overlay the find highlights
        List<ColorRange> list = Calculator.processFindCommand(matcher);
        matcher = pattern.matcher(characterPane.getText());

        try {
            while(matcher.find()) {
                int start = matcher.start(groupName);
                int end = matcher.end(groupName) - 1;
                ColorRange range = new ColorRange(GROUP_COLOR, start, end, true);
                list.add(range);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return list;
    }

    /**
     * checks that the character at the supplied index is not escaped
     */
    private static boolean isNotEscaped(String regex, int index) {
        // count the number of consecutive \ characters preceding the supplied index
        // if this is odd return true
        int count = 0;
        for(int i = index -1; i>=0; i++){
            if(regex.charAt(i) == '\\') {
                count++;
            }
            else{
                break;
            }
        }
        return count % 2 ==0;
    }
}