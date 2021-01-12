function plot_orbit_count(data_file_name)
% Plot number of orbits which exist after each event analyzed.
%
% Parameters:
%   data_file_name - File produced by process_log.py
%
% Returns:
%   none
  
    % Load the data file and assign date and orbit numbers
    event_data = load(data_file_name);
    date_number = datenum( ...
        event_data(:, 1), event_data(:, 2), event_data(:, 3), ...
        event_data(:, 4), event_data(:, 5), event_data(:, 6) ...
    );
    orbit_number = datenum(event_data(:, 7));

    % Plot the orbit number as a function of date number, and print
    figure(1); hold off;
    offset = [0.05, 0.05, 0.05, 0.05];
    gph = [];
    gph = [gph, plot(86400 * (date_number - date_number(1)), orbit_number / 3)]; hold on;
    limits = axis;
    legend({'1 s', '2 s', '4 s', '8 s'}, 'location', 'northwest');
    axis([0, 120, limits(3), limits(4)]);
    set(gca, 'FontName', 'Arial', 'FontSize', 14, 'FontWeight', 'demi');
    set(gph, 'LineWidth', 1.5);
    gth = [];
    set(gth, 'FontName', 'Arial', 'FontSize', 14, 'FontWeight', 'bold', 'HorizontalAlignment', 'left');
    glh = [];
    glh = [glh, xlabel('Run Time')];
    glh = [glh, ylabel('Total States per Object')];
    set(glh, 'FontName', 'Arial', 'FontSize', 14, 'FontWeight', 'demi');
    trim_plot(offset);
    print('plot_orbit_count.pdf', '-dpdf');

end % plot_orbit_count()
