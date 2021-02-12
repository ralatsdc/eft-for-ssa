function trim_plot(off)
% Trims whitespace from a plot.
%
% Parameters:
%   off - Row matrix of positions values to offset tight inset
%   values. Order is left, bottom, right, top, and units are
%   normalized.
%
% Returns:
%   none

    % Set axis properties
    set(gca, 'units', 'normalized')
    % [left, bottom, right, top]
    ins = get(gca, 'TightInset');
    % off = [0.01, 0.01, 0.05, 0.10];
    tot = ins + off;
    % [left, bottom, width, height]
    pos = [tot(1), tot(2), 1 - tot(1) - tot(3), 1 - tot(2) - tot(4)];
    set(gca, 'Position', pos);

    % Set figure properties
    set(gca, 'units', 'centimeters')
    % [left, bottom, right, top]
    ins_cm = get(gca, 'TightInset');
    off_cm([1, 3]) = off([1, 3]) * ins_cm(1) / ins(1);
    off_cm([2, 4]) = off([2, 4]) * ins_cm(2) / ins(2);
    tot_cm = ins_cm + off_cm;
    % [left, bottom, width, height]
    pos_cm = get(gca, 'Position');
    set(gcf, 'PaperUnits', 'centimeters');
    set(gcf, 'PaperSize', [pos_cm(3) + tot_cm(1) + tot_cm(3), pos_cm(4) + tot_cm(2) + tot_cm(4)]);
    set(gcf, 'PaperPositionMode', 'manual');
    set(gcf, 'PaperPosition', [0, 0, pos_cm(3) + tot_cm(1) + tot_cm(3), pos_cm(4) + tot_cm(2) + tot_cm(4)]);

end % trim_plot(off)
