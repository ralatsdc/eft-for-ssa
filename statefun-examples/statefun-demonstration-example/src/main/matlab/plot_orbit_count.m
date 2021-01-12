% gst_14 = load('consumer-14s.dat');
% 
% gst_dnm_14 = datenum(gst_14(:, 1), gst_14(:, 2), gst_14(:, 3), gst_14(:, 4), gst_14(:, 5), gst_14(:, 6));
% gst_onm_14 = gst_14(:, 7);
% 
% gst_42 = load('consumer-42s.dat');
% 
% gst_dnm_42 = datenum(gst_42(:, 1), gst_42(:, 2), gst_42(:, 3), gst_42(:, 4), gst_42(:, 5), gst_42(:, 6));
% gst_onm_42 = gst_42(:, 7);
% 
% leo_2035_03 = load('leo-tle-2020-11-03.dat');
% 
% leo_dnm_2035_03 = datenum(leo_2035_03(:, 1), leo_2035_03(:, 2),
% leo_2035_03(:, 3), leo_2035_03(:, 4), leo_2035_03(:, 5), leo_203%
% 5_03(:, 6));
% leo_onm_2035_03 = leo_2035_03(:, 7);
% 
% leo_2035_04 = load('leo-tle-2020-11-04.dat');
% 
% leo_dnm_2035_04 = datenum(leo_2035_04(:, 1), leo_2035_04(:, 2),
% leo_2035_04(:, 3), leo_2035_04(:, 4), leo_2035_04(:, 5), leo_203%
% 5_04(:, 6));
% leo_onm_2035_04 = leo_2035_04(:, 7);
% 
% leo_2035_05 = load('leo-tle-2020-11-05.dat');
% 
% leo_dnm_2035_05 = datenum(leo_2035_05(:, 1), leo_2035_05(:, 2),
% leo_2035_05(:, 3), leo_2035_05(:, 4), leo_2035_05(:, 5), leo_203%
% 5_05(:, 6));
% leo_onm_2035_05 = leo_2035_05(:, 7);
% 
% leo_2035_06 = load('leo-tle-2020-11-06.dat');
% leo_dnm_2035_06 = datenum(leo_2035_06(:, 1), leo_2035_06(:, 2), leo_2035_06(:, 3), leo_2035_06(:, 4), leo_2035_06(:, 5), leo_2035_06(:, 6));
% leo_onm_2035_06 = leo_2035_06(:, 7);

gs_001 = load('consumer-2020-12-29-001.dat');
gs_002 = load('consumer-2020-12-29-002.dat');
gs_004 = load('consumer-2020-12-29-004.dat');
gs_008 = load('consumer-2020-12-29-008.dat');

dn_001 = datenum(gs_001(:, 1), gs_001(:, 2), gs_001(:, 3), gs_001(:, 4), gs_001(:, 5), gs_001(:, 6));
dn_002 = datenum(gs_002(:, 1), gs_002(:, 2), gs_002(:, 3), gs_002(:, 4), gs_002(:, 5), gs_002(:, 6));
dn_004 = datenum(gs_004(:, 1), gs_004(:, 2), gs_004(:, 3), gs_004(:, 4), gs_004(:, 5), gs_004(:, 6));
dn_008 = datenum(gs_008(:, 1), gs_008(:, 2), gs_008(:, 3), gs_008(:, 4), gs_008(:, 5), gs_008(:, 6));

on_001 = datenum(gs_001(:, 7));
on_002 = datenum(gs_002(:, 7));
on_004 = datenum(gs_004(:, 7));
on_008 = datenum(gs_008(:, 7));

figure(1); hold off;
offset = [0.05, 0.05, 0.05, 0.05];
gph = [];
gth = [];
glh = [];
gph = [gph, plot(86400 * (dn_001 - dn_001(1)), on_001 / 3)]; hold on;
gph = [gph, plot(86400 * (dn_002 - dn_002(1)), on_002 / 3)]; hold on;
gph = [gph, plot(86400 * (dn_004 - dn_004(1)), on_004 / 3)]; hold on;
gph = [gph, plot(86400 * (dn_008 - dn_008(1)), on_008 / 3)]; hold on;
limits = axis;
legend({'1 s', '2 s', '4 s', '8 s'}, 'location', 'northwest');
axis([0, 120, limits(3), limits(4)]);
set(gca, 'FontName', 'Arial', 'FontSize', 14, 'FontWeight', 'demi');
set(gph, 'LineWidth', 1.5);
set(gth, 'FontName', 'Arial', 'FontSize', 14, 'FontWeight', 'bold', 'HorizontalAlignment', 'left');
glh = [glh, xlabel('Run Time')];
glh = [glh, ylabel('Total States per Object')];
set(glh, 'FontName', 'Arial', 'FontSize', 14, 'FontWeight', 'demi');
trim_plot(offset);
print('plot_orbit_count.pdf', '-dpdf');

function trim_plot(off)
  
  set(gca, 'units', 'normalized')
  % [left, bottom, right, top]
  ins = get(gca, 'TightInset');
  % off = [0.01, 0.01, 0.05, 0.10];
  tot = ins + off;
  % [left, bottom, width, height]
  pos = [tot(1), tot(2), 1 - tot(1) - tot(3), 1 - tot(2) - tot(4)];
  set(gca, 'Position', pos);
  
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
